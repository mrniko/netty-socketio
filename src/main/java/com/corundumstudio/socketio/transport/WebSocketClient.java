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
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class WebSocketClient implements SocketIOClient {

    private final UUID sessionId;

    private Channel channel;

    private final Disconnectable disconnectable;

    public WebSocketClient(Channel channel, Disconnectable disconnectable, UUID sessionId) {
        this.channel = channel;
        this.disconnectable = disconnectable;
        this.sessionId = sessionId;
    }

    public Channel getChannel() {
        return channel;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public ChannelFuture sendJsonObject(Object object) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        return send(packet);
    }

    public ChannelFuture send(Packet packet) {
        return channel.write(packet);
    }

    public void disconnect() {
        ChannelFuture future = send(new Packet(PacketType.DISCONNECT));
        future.addListener(ChannelFutureListener.CLOSE);

        disconnectable.onDisconnect(this);
    }

    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

}
