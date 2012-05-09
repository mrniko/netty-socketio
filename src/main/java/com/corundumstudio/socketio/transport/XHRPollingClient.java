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

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.NullChannelFuture;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class XHRPollingClient implements SocketIOClient {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean jsonp;
    private final List<String> messages = new LinkedList<String>();
    private final UUID sessionId;

    private String origin;
    private boolean isKeepAlive;
    private boolean connected;
    private Channel channel;

    private final Disconnectable disconnectable;
    private final Encoder encoder;

    public XHRPollingClient(Encoder encoder, Disconnectable disconnectable, UUID sessionId) {
        this.encoder = encoder;
        this.disconnectable = disconnectable;
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void doReconnect(Channel channel, HttpRequest req) {
        this.isKeepAlive = HttpHeaders.isKeepAlive(req);
        this.origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        this.channel = channel;
        this.connected = true;
        sendPayload();
    }

    private synchronized ChannelFuture sendPayload() {
        if (!connected || messages.isEmpty()) {
            return NullChannelFuture.INSTANCE;
        }
        CharSequence data = encoder.encodePayload(messages);
        messages.clear();
        return write(data);
    }

    private ChannelFuture write(CharSequence message) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
        addHeaders(res);

        res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
        HttpHeaders.setContentLength(res, res.getContent().readableBytes());

        connected = false;
        jsonp = false;
        origin = null;

        if (channel.isConnected()) {
            log.trace("Out message: {} sessionId: {}", new Object[] {message, sessionId});
            ChannelFuture f = channel.write(res);
            if (!isKeepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
            return f;
        }
        return NullChannelFuture.INSTANCE;
    }

    private void addHeaders(HttpResponse res) {
        res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.addHeader(CONNECTION, KEEP_ALIVE);
        if (origin != null) {
            res.addHeader("Access-Control-Allow-Origin", origin);
            res.addHeader("Access-Control-Allow-Credentials", "true");
        }
        if (jsonp) {
            res.addHeader(CONTENT_TYPE, "application/javascript");
        }
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

    public ChannelFuture sendJsonp(String message) {
        jsonp = true;
        return sendUnencoded(message);
    }

    public void disconnect() {
        send(new Packet(PacketType.DISCONNECT));
        disconnectable.onDisconnect(this);
    }

    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

}
