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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.NullChannelFuture;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIORouter;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class WebSocketClient implements SocketIOClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<String> messages = new LinkedList<String>();
    private final UUID sessionId;

    private Channel channel;

    private final SocketIORouter socketIORouter;
    private final Encoder encoder;

    public WebSocketClient(Channel channel, Encoder encoder, SocketIORouter socketIORouter, UUID sessionId) {
        this.channel = channel;
        this.encoder = encoder;
        this.socketIORouter = socketIORouter;
        this.sessionId = sessionId;
    }

    public Channel getChannel() {
        return channel;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    private ChannelFuture sendPayload() {
        CharSequence data = encoder.encodePayload(messages);
        messages.clear();
        return write(data);
    }

    private ChannelFuture write(CharSequence message) {
        WebSocketFrame res = new TextWebSocketFrame(message.toString());

        if (channel.isConnected()) {
            log.trace("Out message: {} sessionId: {}", new Object[] {message, sessionId});
            ChannelFuture f = channel.write(res);
            return f;
        }
        return NullChannelFuture.INSTANCE;
    }

    public ChannelFuture sendJsonObject(Object object) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        return send(packet);
    }

    public ChannelFuture send(Packet packet) {
        try {
            String message = encoder.encodePacket(packet);
            return sendUnencoded(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized ChannelFuture sendUnencoded(String message) {
        messages.add(message);
        return sendPayload();
    }

    public void disconnect() {
        socketIORouter.disconnect(sessionId);
        
    }

    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

}
