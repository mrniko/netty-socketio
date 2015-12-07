/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AttributeKey;
import io.netty.util.internal.PlatformDependent;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.messages.OutPacketMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;
import com.corundumstudio.socketio.store.Store;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.transport.NamespaceClient;

public class ClientHead {

    private static final Logger log = LoggerFactory.getLogger(ClientHead.class);

    public static final AttributeKey<ClientHead> CLIENT = AttributeKey.<ClientHead>valueOf("client");

    private final AtomicBoolean disconnected = new AtomicBoolean();
    private final Map<Namespace, NamespaceClient> namespaceClients = PlatformDependent.newConcurrentHashMap();
    private final Map<Transport, TransportState> channels = new HashMap<Transport, TransportState>();
    private final HandshakeData handshakeData;
    private final UUID sessionId;

    private final Store store;
    private final DisconnectableHub disconnectableHub;
    private final AckManager ackManager;
    private ClientsBox clientsBox;
    private final CancelableScheduler disconnectScheduler;
    private final Configuration configuration;

    private Packet lastBinaryPacket;

    // TODO use lazy set
    private volatile Transport currentTransport;

    public ClientHead(UUID sessionId, AckManager ackManager, DisconnectableHub disconnectable,
            StoreFactory storeFactory, HandshakeData handshakeData, ClientsBox clientsBox, Transport transport, CancelableScheduler disconnectScheduler,
            Configuration configuration) {
        this.sessionId = sessionId;
        this.ackManager = ackManager;
        this.disconnectableHub = disconnectable;
        this.store = storeFactory.createStore(sessionId);
        this.handshakeData = handshakeData;
        this.clientsBox = clientsBox;
        this.currentTransport = transport;
        this.disconnectScheduler = disconnectScheduler;
        this.configuration = configuration;

        channels.put(Transport.POLLING, new TransportState());
        channels.put(Transport.WEBSOCKET, new TransportState());
    }

    public void bindChannel(Channel channel, Transport transport) {
        log.debug("binding channel: {} to transport: {}", channel, transport);

        TransportState state = channels.get(transport);
        Channel prevChannel = state.update(channel);
        if (prevChannel != null) {
            clientsBox.remove(prevChannel);
        }
        clientsBox.add(channel, this);

        sendPackets(transport, channel);
    }

    public String getOrigin() {
        return handshakeData.getSingleHeader(HttpHeaders.Names.ORIGIN);
    }

    public ChannelFuture send(Packet packet) {
        return send(packet, getCurrentTransport());
    }

    public void cancelPingTimeout() {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        disconnectScheduler.cancel(key);
    }

    public void schedulePingTimeout() {
        SchedulerKey key = new SchedulerKey(Type.PING_TIMEOUT, sessionId);
        disconnectScheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                ClientHead client = clientsBox.get(sessionId);
                if (client != null) {
                    client.onChannelDisconnect();
                    log.debug("{} removed due to ping timeout", sessionId);
                }
            }
        }, configuration.getPingTimeout() + configuration.getPingInterval(), TimeUnit.MILLISECONDS);
    }

    public ChannelFuture send(Packet packet, Transport transport) {
        TransportState state = channels.get(transport);
        state.getPacketsQueue().add(packet);

        Channel channel = state.getChannel();
        if (channel == null
                || (transport == Transport.POLLING && channel.attr(EncoderHandler.WRITE_ONCE).get() != null)) {
            return null;
        }
        return sendPackets(transport, channel);
    }

    private ChannelFuture sendPackets(Transport transport, Channel channel) {
        // TODO promise handling
        return channel.writeAndFlush(new OutPacketMessage(this, transport));
    }

    public void removeNamespaceClient(NamespaceClient client) {
        namespaceClients.remove(client.getNamespace());
        if (namespaceClients.isEmpty()) {
            disconnectableHub.onDisconnect(this);
        }
    }

    public NamespaceClient getChildClient(Namespace namespace) {
        return namespaceClients.get(namespace);
    }

    public NamespaceClient addNamespaceClient(Namespace namespace) {
        NamespaceClient client = new NamespaceClient(this, namespace);
        namespaceClients.put(namespace, client);
        return client;
    }

    public Set<Namespace> getNamespaces() {
        return namespaceClients.keySet();
    }

    public boolean isConnected() {
        return !disconnected.get();
    }

    public void onChannelDisconnect() {
        cancelPingTimeout();

        disconnected.set(true);
        for (NamespaceClient client : namespaceClients.values()) {
            client.onDisconnect();
        }
        for (TransportState state : channels.values()) {
            if (state.getChannel() != null) {
                clientsBox.remove(state.getChannel());
            }
        }
    }

    public HandshakeData getHandshakeData() {
        return handshakeData;
    }

    public AckManager getAckManager() {
        return ackManager;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public SocketAddress getRemoteAddress() {
        return handshakeData.getAddress();
    }

    public void disconnect() {
        ChannelFuture future = send(new Packet(PacketType.DISCONNECT));
        future.addListener(ChannelFutureListener.CLOSE);

        onChannelDisconnect();
    }

    public boolean isChannelOpen() {
        for (TransportState state : channels.values()) {
            if (state.getChannel() != null
                    && state.getChannel().isActive()) {
                return true;
            }
        }
        return false;
    }

    public Store getStore() {
        return store;
    }

    public boolean isTransportChannel(Channel channel, Transport transport) {
        TransportState state = channels.get(transport);
        if (state.getChannel() == null) {
            return false;
        }
        return state.getChannel().equals(channel);
    }

    public void upgradeCurrentTransport(Transport currentTransport) {
        TransportState state = channels.get(currentTransport);

        for (Entry<Transport, TransportState> entry : channels.entrySet()) {
            if (!entry.getKey().equals(currentTransport)) {

                Queue<Packet> queue = entry.getValue().getPacketsQueue();
                state.setPacketsQueue(queue);

                sendPackets(currentTransport, state.getChannel());
                this.currentTransport = currentTransport;
                log.debug("Transport upgraded to: {} for: {}", currentTransport, sessionId);
                break;
            }
        }
    }

    public Transport getCurrentTransport() {
        return currentTransport;
    }

    public Queue<Packet> getPacketsQueue(Transport transport) {
        return channels.get(transport).getPacketsQueue();
    }

    public void setLastBinaryPacket(Packet lastBinaryPacket) {
        this.lastBinaryPacket = lastBinaryPacket;
    }
    public Packet getLastBinaryPacket() {
        return lastBinaryPacket;
    }

}
