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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
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
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.HeartbeatHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHROutMessage;
import com.corundumstudio.socketio.parser.ErrorAdvice;
import com.corundumstudio.socketio.parser.ErrorReason;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;

@Sharable
public class XHRPollingTransport extends BaseTransport {

    public static final String NAME = "polling";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, XHRPollingClient> sessionId2Client =
                                                    new ConcurrentHashMap<UUID, XHRPollingClient>();
    private final CancelableScheduler scheduler;

    private final AckManager ackManager;
    private final AuthorizeHandler authorizeHandler;
    private final DisconnectableHub disconnectable;
    private final Configuration configuration;
    private final String path;

    public XHRPollingTransport(String connectPath, AckManager ackManager, DisconnectableHub disconnectable, CancelableScheduler scheduler,
                                AuthorizeHandler authorizeHandler, Configuration configuration, HeartbeatHandler heartbeatHandler) {
        this.path = connectPath + NAME + "/";
        this.ackManager = ackManager;
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.disconnectable = disconnectable;
        this.scheduler = scheduler;
    }

    public static void main(String[] args) {
        ByteBuf s5 = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAj/NDAvY2hhdDE=", CharsetUtil.UTF_8));
        System.out.println(s5.toString(CharsetUtil.UTF_8));
        ByteBuf s4 = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAj/NDAvY2hhdDI=", CharsetUtil.UTF_8));
        System.out.println(s4.toString(CharsetUtil.UTF_8));

        ByteBuf s = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAH/Mw==", CharsetUtil.UTF_8));
        System.out.println(s.toString(CharsetUtil.UTF_8));
        ByteBuf s1 = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAL/NDAACP80MC9jaGF0MQ==", CharsetUtil.UTF_8));
        System.out.println(s1.toString(CharsetUtil.UTF_8));
//        System.out.println(ByteBufUtil.hexDump(s1));
        ByteBuf s2 = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAL/NDA=", CharsetUtil.UTF_8));
        System.out.println(s2.toString(CharsetUtil.UTF_8));
        ByteBuf s3 = io.netty.handler.codec.base64.Base64.decode(Unpooled.copiedBuffer("AAL/NDAAAv80MA==", CharsetUtil.UTF_8));
        System.out.println(s3.toString(CharsetUtil.UTF_8));
//        System.out.println(ByteBufUtil.hexDump(s2));

        System.out.println("sadfsdf".split(",").length);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());

            List<String> transport = queryDecoder.parameters().get("transport");
            List<String> sid = queryDecoder.parameters().get("sid");

            if (transport != null && NAME.equals(transport.get(0))
                    && sid != null && sid.get(0) != null) {
                try {
                    handleMessage(req, sid.get(0), queryDecoder, ctx);
                } finally {
                    req.release();
                }
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    private void handleMessage(FullHttpRequest req, String sid, QueryStringDecoder queryDecoder, ChannelHandlerContext ctx)
                                                                                throws IOException {
            UUID sessionId = UUID.fromString(sid);

            String origin = req.headers().get(HttpHeaders.Names.ORIGIN);
            if (queryDecoder.parameters().containsKey("disconnect")) {
                MainBaseClient client = sessionId2Client.get(sessionId);
                client.onChannelDisconnect();
                ctx.channel().writeAndFlush(new XHROutMessage(origin, sessionId));
            } else if (HttpMethod.POST.equals(req.getMethod())) {
                onPost(sessionId, ctx, origin, req.content());
            } else if (HttpMethod.GET.equals(req.getMethod())) {
                onGet(sessionId, ctx, origin);
            } else if (HttpMethod.OPTIONS.equals(req.getMethod())) {
                onOptions(sessionId, ctx, origin);
            }
    }

    private void onOptions(UUID sessionId, ChannelHandlerContext ctx, String origin) {
        HandshakeData data = authorizeHandler.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        XHRPollingClient client = getClient(sessionId, data);

        ctx.channel().writeAndFlush(new XHROptionsMessage(origin, sessionId));
    }

    private void scheduleNoop(final UUID sessionId) {
        SchedulerKey key = new SchedulerKey(Type.POLLING, sessionId);
        scheduler.cancel(key);
        scheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                XHRPollingClient client = sessionId2Client.get(sessionId);
                if (client != null) {
                    client.send(new Packet(PacketType.BINARY_EVENT));
                }
            }
        }, configuration.getPollingDuration(), TimeUnit.SECONDS);
    }

    private void scheduleDisconnect(Channel channel, final UUID sessionId) {
        final SchedulerKey key = new SchedulerKey(Type.CLOSE_TIMEOUT, sessionId);
        scheduler.cancel(key);
        ChannelFuture future = channel.closeFuture();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                scheduler.scheduleCallback(key, new Runnable() {
                    @Override
                    public void run() {
                        XHRPollingClient client = sessionId2Client.get(sessionId);
                        if (client != null) {
                            client.onChannelDisconnect();
                            log.debug("Client: {} disconnected due to connection timeout", sessionId);
                        }
                    }
                }, configuration.getCloseTimeout(), TimeUnit.SECONDS);
            }
        });
    }

    private void onPost(UUID sessionId, ChannelHandlerContext ctx, String origin, ByteBuf content)
                                                                                throws IOException {
        HandshakeData data = authorizeHandler.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        XHRPollingClient client = getClient(sessionId, data);

        // release POST response before message processing
        ctx.channel().writeAndFlush(new XHROutMessage(origin, sessionId));
        ctx.pipeline().fireChannelRead(new PacketsMessage(client, content));

//        authorizeHandler.connect(client);
    }

    private void onGet(UUID sessionId, ChannelHandlerContext ctx, String origin) {
        HandshakeData data = authorizeHandler.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        XHRPollingClient client = getClient(sessionId, data);
        client.bindChannel(ctx.channel(), origin);
        // TODO implement send packets at one response
        authorizeHandler.connect(client);

        scheduleDisconnect(ctx.channel(), sessionId);
        scheduleNoop(sessionId);
    }

    public XHRPollingClient getClient(UUID sessionId) {
        return sessionId2Client.get(sessionId);
    }

    private XHRPollingClient getClient(UUID sessionId, HandshakeData data) {
        XHRPollingClient client = (XHRPollingClient) sessionId2Client.get(sessionId);
        if (client == null) {
            client = new XHRPollingClient(ackManager, disconnectable, sessionId, configuration.getStoreFactory(), data);
            sessionId2Client.put(sessionId, client);

            log.debug("Client for sessionId: {} was created", sessionId);
        }
        return client;
    }

    private void sendError(ChannelHandlerContext ctx, String origin, UUID sessionId) {
        log.debug("Client with sessionId: {} was not found! Reconnect error response sended", sessionId);
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.CLIENT_NOT_HANDSHAKEN);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        ctx.channel().writeAndFlush(new XHRErrorMessage(packet, origin, sessionId));
    }

    @Override
    public void onDisconnect(MainBaseClient client) {
        if (client instanceof XHRPollingClient) {
            UUID sessionId = client.getSessionId();

            sessionId2Client.remove(sessionId);
            SchedulerKey noopKey = new SchedulerKey(Type.POLLING, sessionId);
            scheduler.cancel(noopKey);
            SchedulerKey closeTimeoutKey = new SchedulerKey(Type.CLOSE_TIMEOUT, sessionId);
            scheduler.cancel(closeTimeoutKey);
        }
    }

    public void onDisconnect(UUID sessionId) {
        XHRPollingClient client = sessionId2Client.get(sessionId);
        onDisconnect(client);
    }

}
