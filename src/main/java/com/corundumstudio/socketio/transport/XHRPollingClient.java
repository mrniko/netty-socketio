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
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.XHRNewChannelMessage;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class XHRPollingClient implements SocketIOClient {

    private final UUID sessionId;

    private String origin;
    private Channel channel;

    private final Disconnectable disconnectable;

    public XHRPollingClient(Disconnectable disconnectable, UUID sessionId) {
        this.disconnectable = disconnectable;
        this.sessionId = sessionId;
    }

    public Channel getChannel() {
        return channel;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void update(Channel channel, HttpRequest req) {
        this.origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        this.channel = channel;
        channel.write(new XHRNewChannelMessage());
    }

    public String getOrigin() {
        return origin;
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
        send(new Packet(PacketType.DISCONNECT));
        disconnectable.onDisconnect(this);
    }

    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

}
