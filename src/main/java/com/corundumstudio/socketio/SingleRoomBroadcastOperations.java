/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.misc.IterableCollection;
import com.corundumstudio.socketio.protocol.EngineIOVersion;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.DispatchMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import io.netty.buffer.Unpooled;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: liangjiaqi
 * Date: 2020/8/8 6:08 PM
 */
public class SingleRoomBroadcastOperations implements BroadcastOperations {
    private final String namespace;
    private final String room;
    private final Iterable<SocketIOClient> clients;
    private final StoreFactory storeFactory;

    public SingleRoomBroadcastOperations(String namespace, String room, Iterable<SocketIOClient> clients, StoreFactory storeFactory) {
        super();
        this.namespace = namespace;
        this.room = room;
        this.clients = clients;
        this.storeFactory = storeFactory;
    }

    private void dispatch(Packet packet) {
        this.storeFactory.pubSubStore().publish(
                PubSubType.DISPATCH,
                new DispatchMessage(this.room, packet, this.namespace));
    }

    @Override
    public Collection<SocketIOClient> getClients() {
        return new IterableCollection<SocketIOClient>(clients);
    }

    @Override
    public void send(Packet packet) {
        for (SocketIOClient client : clients) {
            packet.setEngineIOVersion(client.getEngineIOVersion());
            client.send(packet);
        }
        dispatch(packet);
    }

    @Override
    public <T> void send(Packet packet, BroadcastAckCallback<T> ackCallback) {
        for (SocketIOClient client : clients) {
            client.send(packet, ackCallback.createClientCallback(client));
        }
        ackCallback.loopFinished();
    }

    @Override
    public void disconnect() {
        for (SocketIOClient client : clients) {
            client.disconnect();
        }
    }

    @Override
    public void sendEvent(String name, SocketIOClient excludedClient, @NonNull Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.UNKNOWN);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));

        // handle byte[] data
        handleBytes(packet, data);

        for (SocketIOClient client : clients) {
            packet.setEngineIOVersion(client.getEngineIOVersion());
            if (client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }
            client.send(packet);
        }
        dispatch(packet);
    }

    @Override
    public void sendEvent(String name, @NonNull Object... data) {
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.UNKNOWN);
        packet.setSubType(PacketType.EVENT);
        packet.setName(name);
        packet.setData(Arrays.asList(data));

        // handle byte[] data
        handleBytes(packet, data);

        send(packet);
    }

    private static void handleBytes(Packet packet, Object[] data) {
        List<byte[]> bytes = Arrays.stream(data)
                .filter(o -> o instanceof byte[])
                .map(b -> (byte[]) b)
                .filter(b -> b.length > 0)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(bytes)) {
            packet.initAttachments(bytes.size());
            bytes.stream().peek(b -> packet.addAttachment(Unpooled.wrappedBuffer(b)));
        }
    }

    @Override
    public <T> void sendEvent(String name, Object data, BroadcastAckCallback<T> ackCallback) {
        for (SocketIOClient client : clients) {
            client.sendEvent(name, ackCallback.createClientCallback(client), data);
        }
        ackCallback.loopFinished();
    }

    @Override
    public <T> void sendEvent(String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback) {
        for (SocketIOClient client : clients) {
            if (client.getSessionId().equals(excludedClient.getSessionId())) {
                continue;
            }
            client.sendEvent(name, ackCallback.createClientCallback(client), data);
        }
        ackCallback.loopFinished();
    }
}
