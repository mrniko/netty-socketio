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
package com.corundumstudio.socketio.handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.messages.BaseMessage;
import com.corundumstudio.socketio.messages.HttpMessage;
import com.corundumstudio.socketio.messages.OutPacketMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.protocol.Encoder;
import com.corundumstudio.socketio.protocol.Packet;

@Sharable
public class EncoderHandler extends ChannelOutboundHandlerAdapter {

    private static final byte[] OK = "ok".getBytes(CharsetUtil.UTF_8);

    public static final AttributeKey<String> ORIGIN = AttributeKey.valueOf("origin");
    public static final AttributeKey<String> USER_AGENT = AttributeKey.valueOf("userAgent");
    public static final AttributeKey<Integer> JSONP_INDEX = AttributeKey.valueOf("jsonpIndex");
    public static final AttributeKey<Boolean> WRITE_ONCE = AttributeKey.valueOf("writeOnce");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Encoder encoder;

    public EncoderHandler(Encoder encoder) {
        this.encoder = encoder;
    }

    private void write(XHROptionsMessage msg, Channel channel, ByteBuf out) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        HttpHeaders.addHeader(res, "Set-Cookie", "io=" + msg.getSessionId());
        HttpHeaders.addHeader(res, CONNECTION, KEEP_ALIVE);
        HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE);
        addOriginHeaders(channel, res);

        sendMessage(msg, channel, out, res);
    }


    private void write(XHRPostMessage msg, Channel channel, ByteBuf out) {
        out.writeBytes(OK);
        sendMessage(msg, channel, out, "text/html");
    }

    private void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, String type) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.headers()
            .add(CONTENT_TYPE, type)
            .add("Set-Cookie", "io=" + msg.getSessionId())
            .add(CONNECTION, KEEP_ALIVE);

        addOriginHeaders(channel, res);
        HttpHeaders.setContentLength(res, out.readableBytes());

        // prevent XSS warnings on IE
        // https://github.com/LearnBoost/socket.io/pull/1333
        String userAgent = channel.attr(EncoderHandler.USER_AGENT).get();
        if (userAgent != null
                && (userAgent.contains(";MSIE")
                    || userAgent.contains("Trident/"))) {
            res.headers().add("X-XSS-Protection", "0");
        }

        sendMessage(msg, channel, out, res);
    }

    private void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, HttpResponse res) {
        channel.write(res);

        if (log.isTraceEnabled()) {
            log.trace("Out message: {} - sessionId: {}",
                        out.toString(CharsetUtil.UTF_8), msg.getSessionId());
        }

        if (out.isReadable()) {
            channel.write(out);
        } else {
            out.release();
        }

        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
    }

    private void addOriginHeaders(Channel channel, HttpResponse res) {
        String origin = channel.attr(ORIGIN).get();
        if (origin != null) {
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof BaseMessage)) {
            super.write(ctx, msg, promise);
            return;
        }

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());

        if (msg instanceof OutPacketMessage) {
            OutPacketMessage m = (OutPacketMessage) msg;
            if (m.getTransport() == Transport.WEBSOCKET) {
                handleWebsocket((OutPacketMessage) msg, ctx);
            }
            if (m.getTransport() == Transport.POLLING) {
                handleHTTP((OutPacketMessage) msg, ctx, out);
            }
        }

        if (msg instanceof XHROptionsMessage) {
            write((XHROptionsMessage) msg, ctx.channel(), out);
        }

        if (msg instanceof XHRPostMessage) {
            write((XHRPostMessage) msg, ctx.channel(), out);
        }
    }

    private void handleWebsocket(OutPacketMessage msg, ChannelHandlerContext ctx) throws IOException {
        while (true) {
            Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());
            Packet packet = queue.poll();
            if (packet == null) {
                break;
            }

            ByteBuf out = encoder.allocateBuffer(ctx.alloc());
            encoder.encodePacket(packet, out, ctx.alloc(), true, false);

            WebSocketFrame res = new TextWebSocketFrame(out);
            if (log.isTraceEnabled()) {
                log.trace("Out message: {} sessionId: {}", out.toString(CharsetUtil.UTF_8), msg.getSessionId());
            }
            ctx.channel().writeAndFlush(res);
            if (!out.isReadable()) {
                out.release();
            }
        }

    }

    private void handleHTTP(OutPacketMessage msg, ChannelHandlerContext ctx, ByteBuf out) throws IOException {
        Channel channel = ctx.channel();
        Attribute<Boolean> attr = channel.attr(WRITE_ONCE);

        Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());

        if (!channel.isActive()
                || queue.isEmpty()
                    || !attr.compareAndSet(null, true)) {
            out.release();
            return;
        }

        Integer jsonpIndex = ctx.channel().attr(EncoderHandler.JSONP_INDEX).get();
        if (jsonpIndex != null) {
            encoder.encodeJsonP(jsonpIndex, queue, out, ctx.alloc(), 50);
            sendMessage(msg, channel, out, "application/javascript");
        } else {
            encoder.encodePackets(queue, out, ctx.alloc(), 50);
            sendMessage(msg, channel, out, "application/octet-stream");
        }
    }

}
