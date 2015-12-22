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
import io.netty.buffer.ByteBufUtil;
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
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Queue;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.messages.BaseMessage;
import com.corundumstudio.socketio.messages.HttpMessage;
import com.corundumstudio.socketio.messages.OutPacketMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketEncoder;

@Sharable
public class EncoderHandler extends ChannelOutboundHandlerAdapter {

    private static final byte[] OK = "ok".getBytes(CharsetUtil.UTF_8);

    public static final AttributeKey<String> ORIGIN = AttributeKey.valueOf("origin");
    public static final AttributeKey<String> USER_AGENT = AttributeKey.valueOf("userAgent");
    public static final AttributeKey<Boolean> B64 = AttributeKey.valueOf("b64");
    public static final AttributeKey<Integer> JSONP_INDEX = AttributeKey.valueOf("jsonpIndex");
    public static final AttributeKey<Boolean> WRITE_ONCE = AttributeKey.valueOf("writeOnce");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PacketEncoder encoder;

    private String version;
    private Configuration configuration;

    public EncoderHandler(Configuration configuration, PacketEncoder encoder) throws IOException {
        this.encoder = encoder;
        this.configuration = configuration;

        if (configuration.isAddVersionHeader()) {
            readVersion();
        }
    }

    private void readVersion() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                Attributes attrs = manifest.getMainAttributes();
                if (attrs == null) {
                    continue;
                }
                String name = attrs.getValue("Bundle-Name");
                if (name != null && name.equals("netty-socketio")) {
                    version = name + "/" + attrs.getValue("Bundle-Version");
                    break;
                }
            } catch (IOException E) {
                // skip it
            }
        }
    }

    private void write(XHROptionsMessage msg, ChannelHandlerContext ctx, ChannelPromise promise) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        HttpHeaders.addHeader(res, "Set-Cookie", "io=" + msg.getSessionId());
        HttpHeaders.addHeader(res, CONNECTION, KEEP_ALIVE);
        HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_HEADERS, CONTENT_TYPE);
        addOriginHeaders(ctx.channel(), res);

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        sendMessage(msg, ctx.channel(), out, res, promise);
    }

    private void write(XHRPostMessage msg, ChannelHandlerContext ctx, ChannelPromise promise) {
        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        out.writeBytes(OK);
        sendMessage(msg, ctx.channel(), out, "text/html", promise);
    }

    private void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, String type, ChannelPromise promise) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.headers().add(CONTENT_TYPE, type).add("Set-Cookie", "io=" + msg.getSessionId())
                .add(CONNECTION, KEEP_ALIVE);

        addOriginHeaders(channel, res);
        HttpHeaders.setContentLength(res, out.readableBytes());

        // prevent XSS warnings on IE
        // https://github.com/LearnBoost/socket.io/pull/1333
        String userAgent = channel.attr(EncoderHandler.USER_AGENT).get();
        if (userAgent != null && (userAgent.contains(";MSIE") || userAgent.contains("Trident/"))) {
            res.headers().add("X-XSS-Protection", "0");
        }

        sendMessage(msg, channel, out, res, promise);
    }

    private void sendMessage(HttpMessage msg, Channel channel, ByteBuf out, HttpResponse res, ChannelPromise promise) {
        channel.write(res);

        if (log.isTraceEnabled()) {
            log.trace("Out message: {} - sessionId: {}", out.toString(CharsetUtil.UTF_8), msg.getSessionId());
        }

        if (out.isReadable()) {
            channel.write(out);
        } else {
            out.release();
        }

        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise).addListener(ChannelFutureListener.CLOSE);
    }

    private void addOriginHeaders(Channel channel, HttpResponse res) {
        if (version != null) {
            res.headers().add(HttpHeaders.Names.SERVER, version);
        }

        if (configuration.getOrigin() != null) {
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, configuration.getOrigin());
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
        } else {
            String origin = channel.attr(ORIGIN).get();
            if (origin != null) {
                HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE);
            } else {
                HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof BaseMessage)) {
            super.write(ctx, msg, promise);
            return;
        }

        if (msg instanceof OutPacketMessage) {
            OutPacketMessage m = (OutPacketMessage) msg;
            if (m.getTransport() == Transport.WEBSOCKET) {
                handleWebsocket((OutPacketMessage) msg, ctx, promise);
            }
            if (m.getTransport() == Transport.POLLING) {
                handleHTTP((OutPacketMessage) msg, ctx, promise);
            }
        } else if (msg instanceof XHROptionsMessage) {
            write((XHROptionsMessage) msg, ctx, promise);
        } else if (msg instanceof XHRPostMessage) {
            write((XHRPostMessage) msg, ctx, promise);
        }
    }

    private void handleWebsocket(final OutPacketMessage msg, ChannelHandlerContext ctx, ChannelPromise promise) throws IOException {
        while (true) {
            Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());
            Packet packet = queue.poll();
            if (packet == null) {
                promise.setSuccess();
                break;
            }

            final ByteBuf out = encoder.allocateBuffer(ctx.alloc());
            encoder.encodePacket(packet, out, ctx.alloc(), true);

            WebSocketFrame res = new TextWebSocketFrame(out);
            if (log.isTraceEnabled()) {
                log.trace("Out message: {} sessionId: {}", out.toString(CharsetUtil.UTF_8), msg.getSessionId());
            }

            if (out.isReadable()) {
                ctx.channel().writeAndFlush(res, promise);
            } else {
                promise.setSuccess();
                out.release();
            }

            for (ByteBuf buf : packet.getAttachments()) {
                ByteBuf outBuf = encoder.allocateBuffer(ctx.alloc());
                outBuf.writeByte(4);
                outBuf.writeBytes(buf);
                if (log.isTraceEnabled()) {
                    log.trace("Out attachment: {} sessionId: {}", ByteBufUtil.hexDump(outBuf), msg.getSessionId());
                }
                ctx.channel().writeAndFlush(new BinaryWebSocketFrame(outBuf));
            }
        }
    }

    private void handleHTTP(OutPacketMessage msg, ChannelHandlerContext ctx, ChannelPromise promise) throws IOException {
        Channel channel = ctx.channel();
        Attribute<Boolean> attr = channel.attr(WRITE_ONCE);

        Queue<Packet> queue = msg.getClientHead().getPacketsQueue(msg.getTransport());

        if (!channel.isActive() || queue.isEmpty() || !attr.compareAndSet(null, true)) {
            promise.setSuccess();
            return;
        }

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());
        Boolean b64 = ctx.channel().attr(EncoderHandler.B64).get();
        if (b64 != null && b64) {
            Integer jsonpIndex = ctx.channel().attr(EncoderHandler.JSONP_INDEX).get();
            encoder.encodeJsonP(jsonpIndex, queue, out, ctx.alloc(), 50);
            String type = "application/javascript";
            if (jsonpIndex == null) {
                type = "text/plain";
            }
            sendMessage(msg, channel, out, type, promise);
        } else {
            encoder.encodePackets(queue, out, ctx.alloc(), 50);
            sendMessage(msg, channel, out, "application/octet-stream", promise);
        }
    }

}
