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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthorizeHandler;
import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.PacketListener;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Packet;

public class WebSocketTransport extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, WebSocketClient> sessionId2Client = new ConcurrentHashMap<UUID, WebSocketClient>();
    private final Map<Integer, WebSocketClient> channelId2Client = new ConcurrentHashMap<Integer, WebSocketClient>();

    private final AuthorizeHandler authorizeHandler;
    private final Disconnectable disconnectable;
    private final PacketListener packetListener;
    private final Decoder decoder;
    private final String path;


    public WebSocketTransport(String connectPath, Decoder decoder,
            Disconnectable disconnectable, PacketListener packetListener, AuthorizeHandler authorizeHandler) {
        this.path = connectPath + "websocket";
        this.decoder = decoder;
        this.authorizeHandler = authorizeHandler;
        this.disconnectable = disconnectable;
        this.packetListener = packetListener;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof CloseWebSocketFrame) {
            ctx.getChannel().close();
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            WebSocketClient client = channelId2Client.get(ctx.getChannel().getId());
            String content = frame.getText();

            log.trace("In message: {} sessionId: {}", new Object[] {content, client.getSessionId()});

            List<Packet> packets = decoder.decodePayload(content);
            for (Packet packet : packets) {
                packetListener.onPacket(packet, client);
            }
        } else if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
            WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
            if (handshaker != null) {
            	handshaker.handshake(ctx.getChannel(), req);
                QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
                connectClient(ctx.getChannel(), queryDecoder);
            } else {
                factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

    private void connectClient(Channel channel, QueryStringDecoder queryDecoder) {
        String path = queryDecoder.getPath();
        if (!path.startsWith(this.path)) {
            return;
        }

        String[] parts = path.split("/");
        if (parts.length > 3) {
            UUID sessionId = UUID.fromString(parts[4]);
            if (!authorizeHandler.isSessionAuthorized(sessionId)) {
                log.warn("Unauthorized client with sessionId: {}, from ip: {}. Channel closed!",
                        new Object[] {sessionId, channel.getRemoteAddress()});
                channel.close();
                return;
            }

            WebSocketClient client = new WebSocketClient(channel, disconnectable, sessionId);
            channelId2Client.put(channel.getId(), client);
            sessionId2Client.put(sessionId, client);
            authorizeHandler.connect(client);
        } else {
            log.warn("Wrong GET request path: {}, from ip: {}. Channel closed!",
                    new Object[] {path, channel.getRemoteAddress()});
            channel.close();
        }
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + path;
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        if (client instanceof WebSocketClient) {
            WebSocketClient webClient = (WebSocketClient) client;
            sessionId2Client.remove(webClient.getSessionId());
            channelId2Client.remove(webClient.getChannel().getId());
        }
    }

}
