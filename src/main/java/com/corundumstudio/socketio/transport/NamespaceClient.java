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

import java.net.SocketAddress;
import java.util.Collections;
import java.util.UUID;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class NamespaceClient implements SocketIOClient {

    private final BaseClient baseClient;
    private final Namespace namespace;

    public NamespaceClient(BaseClient baseClient, Namespace namespace) {
        this.baseClient = baseClient;
        this.namespace = namespace;
        namespace.addClient(this);
    }

    public BaseClient getBaseClient() {
        return baseClient;
    }

    @Override
    public Transport getTransport() {
        return baseClient.getTransport();
    }

    @Override
    public boolean isChannelOpen() {
        return baseClient.getChannel().isOpen();
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public void sendEvent(String name, Object data) {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setName(name);
        packet.setArgs(Collections.singletonList(data));
        send(packet);
    }

    @Override
    public void sendEvent(String name, Object data, AckCallback<?> ackCallback) {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setName(name);
        packet.setArgs(Collections.singletonList(data));
        send(packet, ackCallback);
    }

    @Override
    public void sendMessage(String message, AckCallback<?> ackCallback) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setData(message);
        send(packet, ackCallback);
    }

    @Override
    public void sendMessage(String message) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setData(message);
        send(packet);
    }

    @Override
    public void sendJsonObject(Object object) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        send(packet);
    }

    @Override
    public void send(Packet packet, AckCallback<?> ackCallback) {
        long index = baseClient.getAckManager().registerAck(getSessionId(), ackCallback);
        packet.setId(index);
        if (!ackCallback.getResultClass().equals(Void.class)) {
            packet.setAck(Packet.ACK_DATA);
        }
        send(packet);
    }

    @Override
    public void send(Packet packet) {
        packet.setEndpoint(namespace.getName());
        baseClient.send(packet);
    }

    @Override
    public void sendJsonObject(Object object, AckCallback<?> ackCallback) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        send(packet, ackCallback);
    }

    public void onDisconnect() {
        namespace.onDisconnect(this);
        baseClient.removeChildClient(this);
    }

    @Override
    public void disconnect() {
        send(new Packet(PacketType.DISCONNECT));
        onDisconnect();
    }

    @Override
    public UUID getSessionId() {
        return baseClient.getSessionId();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return baseClient.getRemoteAddress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSessionId() == null) ? 0 : getSessionId().hashCode());
        result = prime * result
                + ((getNamespace().getName() == null) ? 0 : getNamespace().getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NamespaceClient other = (NamespaceClient) obj;
        if (getSessionId() == null) {
            if (other.getSessionId() != null)
                return false;
        } else if (!getSessionId().equals(other.getSessionId()))
            return false;
        if (getNamespace().getName() == null) {
            if (other.getNamespace().getName() != null)
                return false;
        } else if (!getNamespace().getName().equals(other.getNamespace().getName()))
            return false;
        return true;
    }

    @Override
    public <T> void joinRoom(T roomKey) {
        namespace.joinRoom(roomKey, this);
    }

    @Override
    public <T> void leaveRoom(T roomKey) {
        namespace.leaveRoom(roomKey, this);
    }

}
