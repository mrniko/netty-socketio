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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.ReferenceCountUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOChannelInitializer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.HeartbeatHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.store.StoreFactory;

@Sharable
public class WebSocketTransport extends BaseTransport {

    public static final String NAME = "websocket";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, WebSocketClient> sessionId2Client = new ConcurrentHashMap<UUID, WebSocketClient>();
    private final Map<Channel, WebSocketClient> channelId2Client = new ConcurrentHashMap<Channel, WebSocketClient>();

    private final AckManager ackManager;
    private final HeartbeatHandler heartbeatHandler;
    private final AuthorizeHandler authorizeHandler;
    private final DisconnectableHub disconnectableHub;
    private final StoreFactory storeFactory;
    private final CancelableScheduler scheduler;
    private final Configuration configuration;
    private final XHRPollingTransport pollingTransport;

    private final boolean isSsl;

    public WebSocketTransport(boolean isSsl, AckManager ackManager, DisconnectableHub disconnectable,
            AuthorizeHandler authorizeHandler, HeartbeatHandler heartbeatHandler, StoreFactory storeFactory, Configuration configuration,
            CancelableScheduler scheduler, XHRPollingTransport pollingTransport) {
        this.isSsl = isSsl;
        this.authorizeHandler = authorizeHandler;
        this.ackManager = ackManager;
        this.disconnectableHub = disconnectable;
        this.heartbeatHandler = heartbeatHandler;
        this.storeFactory = storeFactory;
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.pollingTransport = pollingTransport;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof CloseWebSocketFrame) {
            ctx.channel().close();
            ReferenceCountUtil.release(msg);
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            WebSocketClient client = channelId2Client.get(ctx.channel());
            if (client == null) {
                log.debug("Client with was already disconnected. Channel closed!");
                ctx.channel().close();
                frame.release();
                return;
            }

            ctx.pipeline().fireChannelRead(new PacketsMessage(client, frame.content()));
            frame.release();
        } else if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            String path = queryDecoder.path();
            List<String> transport = queryDecoder.parameters().get("transport");
            List<String> sid = queryDecoder.parameters().get("sid");

            if (transport != null && NAME.equals(transport.get(0))
                    && sid != null && sid.get(0) != null) {
                handshake(ctx, sid.get(0), path, req);
                req.release();
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        WebSocketClient client = channelId2Client.get(ctx.channel());
        if (client != null) {
            client.onChannelDisconnect();
        } else {
            super.channelInactive(ctx);
        }
    }

    private void handshake(ChannelHandlerContext ctx, String sid, String path, FullHttpRequest req) {
        final Channel channel = ctx.channel();
        final UUID sessionId = UUID.fromString(sid);

        WebSocketServerHandshakerFactory factory =
                new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false, configuration.getMaxFramePayloadLength());
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        if (handshaker != null) {
            ChannelFuture f = handshaker.handshake(channel, req);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    connectClient(channel, sessionId);
                }
            });
        } else {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
    }

    private void connectClient(final Channel channel, final UUID sessionId) {
        HandshakeData data = authorizeHandler.getHandshakeData(sessionId);
        if (data == null) {
            log.warn("Unauthorized client with sessionId: {}, from ip: {}. Channel closed!",
                        sessionId, channel.remoteAddress());
            channel.close();
            return;
        }

        WebSocketClient client = new WebSocketClient(channel, ackManager, disconnectableHub, sessionId, getTransport(), storeFactory, data);
        // TODO refactor, client with sessionId should be independent from transport
        XHRPollingClient oldClient = pollingTransport.getClient(sessionId);
        if (oldClient != null) {
            for (Namespace namespace : oldClient.getNamespaces()) {
                client.addChildClient(namespace);
            }
        } else {
            authorizeHandler.connect(client);
        }

        channelId2Client.put(channel, client);
        sessionId2Client.put(sessionId, client);

        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.UPGRADE_TIMEOUT, sessionId);
        scheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                XHRPollingTransport transport = channel.pipeline().remove(XHRPollingTransport.class);
                transport.onDisconnect(sessionId);
            }
        }, configuration.getUpgradeTimeout(), TimeUnit.MILLISECONDS);

        heartbeatHandler.onHeartbeat(client);
    }

    protected Transport getTransport() {
        return Transport.WEBSOCKET;
    }

    private String getWebSocketLocation(HttpRequest req) {
        String protocol = "ws://";
        if (isSsl) {
            protocol = "wss://";
        }
        return protocol + req.headers().get(HttpHeaders.Names.HOST) + req.getUri();
    }

    @Override
    public void onDisconnect(MainBaseClient client) {
        if (client instanceof WebSocketClient) {
            WebSocketClient webClient = (WebSocketClient) client;
            sessionId2Client.remove(webClient.getSessionId());
            channelId2Client.remove(webClient.getChannel());
        }
    }

}
