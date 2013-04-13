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
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HeartbeatHandler;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOPipelineFactory;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;

@Sharable
public class WebSocketTransport extends BaseTransport {

    public static final String NAME = "websocket";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, WebSocketClient> sessionId2Client = new ConcurrentHashMap<UUID, WebSocketClient>();
    private final Map<Integer, WebSocketClient> channelId2Client = new ConcurrentHashMap<Integer, WebSocketClient>();

    private final AckManager ackManager;
    private final HeartbeatHandler heartbeatHandler;
    private final AuthorizeHandler authorizeHandler;
    private final DisconnectableHub disconnectableHub;
    private final boolean isSsl;
    protected String path;


    public WebSocketTransport(String connectPath, boolean isSsl, AckManager ackManager, DisconnectableHub disconnectable,
            AuthorizeHandler authorizeHandler, HeartbeatHandler heartbeatHandler) {
        this.path = connectPath + NAME;
        this.isSsl = isSsl;
        this.authorizeHandler = authorizeHandler;
        this.ackManager = ackManager;
        this.disconnectableHub = disconnectable;
        this.heartbeatHandler = heartbeatHandler;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof CloseWebSocketFrame) {
            ctx.getChannel().close();
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            receivePackets(ctx, frame.getBinaryData());
        } else if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            String path = queryDecoder.getPath();
            if (path.startsWith(this.path)) {
                handshake(ctx, path, req);
            } else {
                ctx.sendUpstream(e);
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        WebSocketClient client = channelId2Client.get(ctx.getChannel().getId());
        if (client != null) {
            client.onChannelDisconnect();
        } else {
            super.channelDisconnected(ctx, e);
        }
    }

    private void handshake(ChannelHandlerContext ctx, String path, HttpRequest req) {
        Channel channel = ctx.getChannel();
        String[] parts = path.split("/");
        if (parts.length <= 3) {
            log.warn("Wrong GET request path: {}, from ip: {}. Channel closed!",
                    new Object[] {path, channel.getRemoteAddress()});
            channel.close();
            return;
        }

        UUID sessionId = UUID.fromString(parts[4]);

        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, false);
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        if (handshaker != null) {
            handshaker.handshake(channel, req);
            connectClient(channel, sessionId);
        } else {
            factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        }
    }

    private void receivePackets(ChannelHandlerContext ctx, ChannelBuffer channelBuffer) throws IOException {
        WebSocketClient client = channelId2Client.get(ctx.getChannel().getId());
        Channels.fireMessageReceived(ctx.getChannel(), new PacketsMessage(client, channelBuffer));
    }

    private void connectClient(Channel channel, UUID sessionId) {
        if (!authorizeHandler.isSessionAuthorized(sessionId)) {
            log.warn("Unauthorized client with sessionId: {}, from ip: {}. Channel closed!", new Object[] {
                    sessionId, channel.getRemoteAddress()});
            channel.close();
            return;
        }

        WebSocketClient client = new WebSocketClient(channel, ackManager, disconnectableHub, sessionId, getTransport());

        channelId2Client.put(channel.getId(), client);
        sessionId2Client.put(sessionId, client);
        authorizeHandler.connect(client);

        heartbeatHandler.onHeartbeat(client);
        removeHandler(channel.getPipeline());
    }

    protected Transport getTransport() {
        return Transport.WEBSOCKET;
    }
    
    protected void removeHandler(ChannelPipeline pipeline) {
        pipeline.remove(SocketIOPipelineFactory.FLASH_SOCKET_TRANSPORT);
    }

    private String getWebSocketLocation(HttpRequest req) {
        String protocol = "ws://";
        if (isSsl) {
            protocol = "wss://";
        }
        return protocol + req.getHeader(HttpHeaders.Names.HOST) + req.getUri();
    }

    @Override
    public void onDisconnect(BaseClient client) {
        if (client instanceof WebSocketClient) {
            WebSocketClient webClient = (WebSocketClient) client;
            sessionId2Client.remove(webClient.getSessionId());
            channelId2Client.remove(webClient.getChannel().getId());
        }
    }

    public Iterable<SocketIOClient> getAllClients() {
        Collection<WebSocketClient> clients = sessionId2Client.values();
        return getAllClients(clients);
    }

}
