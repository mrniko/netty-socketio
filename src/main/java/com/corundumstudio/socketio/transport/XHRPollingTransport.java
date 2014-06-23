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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.handler.ClientsBox;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.protocol.ErrorAdvice;
import com.corundumstudio.socketio.protocol.ErrorReason;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;

@Sharable
public class XHRPollingTransport extends ChannelInboundHandlerAdapter {

    public static final String NAME = "polling";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CancelableScheduler scheduler;

    private final Configuration configuration;
    private final ClientsBox clientsBox;
    private final AuthorizeHandler authorizeHandler;

    public XHRPollingTransport(CancelableScheduler scheduler,
                                AuthorizeHandler authorizeHandler, Configuration configuration, ClientsBox clientsBox) {
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.scheduler = scheduler;
        this.clientsBox = clientsBox;
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

            if (transport != null && NAME.equals(transport.get(0))) {
                try {
                    if (sid != null && sid.get(0) != null) {
                        final UUID sessionId = UUID.fromString(sid.get(0));
                        handleMessage(req, sessionId, queryDecoder, ctx);
                    } else {
                        // first connection
                        ClientHead client = ctx.channel().attr(ClientHead.CLIENT).get();
                        handleMessage(req, client.getSessionId(), queryDecoder, ctx);
                    }
                } finally {
                    req.release();
                }
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }

    private void handleMessage(FullHttpRequest req, UUID sessionId, QueryStringDecoder queryDecoder, ChannelHandlerContext ctx)
                                                                                throws IOException {
            String origin = req.headers().get(HttpHeaders.Names.ORIGIN);
            if (queryDecoder.parameters().containsKey("disconnect")) {
                ClientHead client = clientsBox.get(sessionId);
                client.onChannelDisconnect();
                ctx.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));
            } else if (HttpMethod.POST.equals(req.getMethod())) {
                onPost(sessionId, ctx, origin, req.content());
            } else if (HttpMethod.GET.equals(req.getMethod())) {
                onGet(sessionId, ctx, origin);
            } else if (HttpMethod.OPTIONS.equals(req.getMethod())) {
                onOptions(sessionId, ctx, origin);
            }
    }

    private void onOptions(UUID sessionId, ChannelHandlerContext ctx, String origin) {
        HandshakeData data = clientsBox.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        ctx.channel().writeAndFlush(new XHROptionsMessage(origin, sessionId));
    }

    private void onPost(UUID sessionId, ChannelHandlerContext ctx, String origin, ByteBuf content)
                                                                                throws IOException {
        HandshakeData data = clientsBox.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        ClientHead client = clientsBox.get(sessionId);

        // release POST response before message processing
        ctx.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));
        ctx.pipeline().fireChannelRead(new PacketsMessage(client, content, Transport.POLLING));
    }

    private void onGet(UUID sessionId, ChannelHandlerContext ctx, String origin) {
        HandshakeData data = clientsBox.getHandshakeData(sessionId);
        if (data == null) {
            sendError(ctx, origin, sessionId);
            return;
        }

        ClientHead client = clientsBox.get(sessionId);
        client.bindChannel(ctx.channel(), Transport.POLLING);
        // TODO implement send packets at one response
        authorizeHandler.connect(client);
    }

    private void sendError(ChannelHandlerContext ctx, String origin, UUID sessionId) {
        log.debug("Client with sessionId: {} was not found! Reconnect error response sended", sessionId);
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.CLIENT_NOT_HANDSHAKEN);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        ctx.channel().writeAndFlush(new XHRErrorMessage(packet, origin, sessionId));
    }

}
