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
package com.corundumstudio.socketio.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.Store;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.StoreFactory;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

/**
 * Base class for main client.
 *
 * Each main client can have multiple namespace clients, when all namespace
 * clients has disconnected then main client disconnects too.
 *
 *
 */
public abstract class MainBaseClient {

    private final ConcurrentMap<Namespace, SocketIOClient> namespaceClients = new ConcurrentHashMap<Namespace, SocketIOClient>();
    private final Store store;

    private final DisconnectableHub disconnectable;
    private final AckManager ackManager;
    private final UUID sessionId;
    private final Transport transport;
    private Channel channel;

    public MainBaseClient(UUID sessionId, AckManager ackManager, DisconnectableHub disconnectable,
            Transport transport, StoreFactory storeFactory) {
        this.sessionId = sessionId;
        this.ackManager = ackManager;
        this.disconnectable = disconnectable;
        this.transport = transport;
        this.store = storeFactory.create(sessionId);
    }

    public Transport getTransport() {
        return transport;
    }

    public abstract ChannelFuture send(Packet packet);

    public void removeChildClient(SocketIOClient client) {
        namespaceClients.remove((Namespace) client.getNamespace());
        if (namespaceClients.isEmpty()) {
            disconnectable.onDisconnect(this);
        }
    }

    public SocketIOClient getChildClient(Namespace namespace) {
        SocketIOClient client = namespaceClients.get(namespace);
        if (client == null) {
            client = new NamespaceClient(this, namespace);
            SocketIOClient oldClient = namespaceClients.putIfAbsent(namespace, client);
            if (oldClient != null) {
                client = oldClient;
            }
        }
        return client;
    }

    public Collection<SocketIOClient> getAllChildClients() {
        return namespaceClients.values();
    }

    public void onChannelDisconnect() {
        for (SocketIOClient client : getAllChildClients()) {
            ((NamespaceClient) client).onDisconnect();
        }
    }

    public AckManager getAckManager() {
        return ackManager;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    public void disconnect() {
        ChannelFuture future = send(new Packet(PacketType.DISCONNECT));
        future.addListener(ChannelFutureListener.CLOSE);

        onChannelDisconnect();
    }

    Channel getChannel() {
        return channel;
    }

    void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Store getStore() {
        return store;
    }

}
