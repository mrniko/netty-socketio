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
import java.util.UUID;

import org.jboss.netty.channel.Channel;

import com.corundumstudio.socketio.AckManager;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

abstract class BaseClient implements SocketIOClient {

    protected final UUID sessionId;
    protected final AckManager ackManager;
    protected Channel channel;

    public BaseClient(UUID sessionId, AckManager ackManager) {
        this.sessionId = sessionId;
        this.ackManager = ackManager;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public void sendMessage(String message, Runnable callback) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setData(message);
        send(packet, callback);
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
    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    @Override
    public void send(Packet packet, Runnable callback) {
        long index = ackManager.registerAck(sessionId, callback);
        packet.setId(index);
        send(packet);
    }

    @Override
    public void sendJsonObject(Object object, Runnable callback) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        send(packet, callback);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
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
        BaseClient other = (BaseClient) obj;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

}
