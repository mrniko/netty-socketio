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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthorizeHandler;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.HeartbeatHandler;
import com.corundumstudio.socketio.PacketListener;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.ErrorAdvice;
import com.corundumstudio.socketio.parser.ErrorReason;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class XHRPollingTransport extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, XHRPollingClient> sessionId2Client = new ConcurrentHashMap<UUID, XHRPollingClient>();

    private final AuthorizeHandler authorizeHandler;
    private final HeartbeatHandler heartbeatHandler;
    private final PacketListener packetListener;
    private final Disconnectable disconnectable;
    private final Decoder decoder;
    private final String path;
    private final Configuration configuration;

    public XHRPollingTransport(String connectPath, Decoder decoder,
                                PacketListener packetListener, Disconnectable disconnectable,
                                HeartbeatHandler heartbeatHandler, AuthorizeHandler authorizeHandler, Configuration configuration) {
        this.path = connectPath + "xhr-polling/";
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.heartbeatHandler = heartbeatHandler;
        this.disconnectable = disconnectable;
        this.decoder = decoder;
        this.packetListener = packetListener;
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());

            Channel channel = ctx.getChannel();
            if (queryDecoder.getPath().startsWith(path)) {
                String[] parts = queryDecoder.getPath().split("/");
                if (parts.length > 3) {
                    UUID sessionId = UUID.fromString(parts[4]);
                    if (HttpMethod.POST.equals(req.getMethod())) {
                        onPost(sessionId, channel, req);
                    } else if (HttpMethod.GET.equals(req.getMethod())) {
                        onGet(sessionId, channel, req);
                    }
                    if (queryDecoder.getParameters().containsKey("disconnect")) {
                        XHRPollingClient client = sessionId2Client.get(sessionId);
                        disconnectable.onDisconnect(client);
                    }
                } else {
                    log.warn("Wrong {} method request path: {}, from ip: {}. Channel closed!",
                            new Object[] {req.getMethod(), path, channel.getRemoteAddress()});
                    channel.close();
                }
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private void onPost(UUID sessionId, Channel channel, HttpRequest req) throws IOException {
        XHRPollingClient client = sessionId2Client.get(sessionId);
        if (client == null) {
            log.debug("Client with sessionId: {} was already disconnected. Channel closed!", sessionId);
            channel.close();
            return;
        }

        String content = req.getContent().toString(CharsetUtil.UTF_8);
        log.trace("In message: {} sessionId: {}", new Object[] {content, sessionId});
        List<Packet> packets = decoder.decodePayload(content);
        for (Packet packet : packets) {
            packetListener.onPacket(packet, client);
        }

        String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        channel.write(new XHRPostMessage(origin));
    }

    private void onGet(UUID sessionId, Channel channel, HttpRequest req) {
        if (!authorizeHandler.isSessionAuthorized(sessionId)) {
            sendError(channel, req, sessionId);
            return;
        }
        XHRPollingClient client = sessionId2Client.get(sessionId);
        if (client == null) {
            client = createClient(req, channel, sessionId);
        }

        client.update(channel, req);
    }

    private XHRPollingClient createClient(HttpRequest req, Channel channel, UUID sessionId) {
        XHRPollingClient client = new XHRPollingClient(authorizeHandler, sessionId);
        sessionId2Client.put(sessionId, client);
        client.update(channel, req);

        authorizeHandler.connect(client);
        if (configuration.isHeartbeatsEnabled()) {
            heartbeatHandler.sendHeartbeat(client);
        }
        log.debug("Client for sessionId: {} was created", sessionId);
        return client;
    }

    private void sendError(Channel channel, HttpRequest req, UUID sessionId) {
        log.debug("Client with sessionId: {} was not found! Reconnect error response sended", sessionId);
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.CLIENT_NOT_HANDSHAKEN);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        channel.write(new XHRErrorMessage(packet, req.getHeader(HttpHeaders.Names.ORIGIN)));
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        if (client instanceof XHRPollingClient) {
            XHRPollingClient xhrClient = (XHRPollingClient) client;
            sessionId2Client.remove(xhrClient.getSessionId());
        }
    }

}
