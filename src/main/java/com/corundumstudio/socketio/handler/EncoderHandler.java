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
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.AuthorizeMessage;
import com.corundumstudio.socketio.messages.BaseMessage;
import com.corundumstudio.socketio.messages.HttpMessage;
import com.corundumstudio.socketio.messages.WebSocketPacketMessage;
import com.corundumstudio.socketio.messages.WebsocketErrorMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHROutMessage;
import com.corundumstudio.socketio.messages.XHRSendPacketsMessage;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.transport.XHRPollingClient;

@Sharable
public class EncoderHandler extends ChannelOutboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Encoder encoder;

    public EncoderHandler(Encoder encoder) {
        this.encoder = encoder;
    }

    private void write(XHRSendPacketsMessage msg, ChannelHandlerContext ctx, ByteBuf out) throws IOException {
        Channel channel = ctx.channel();
        Attribute<Boolean> attr = channel.attr(XHRPollingClient.WRITE_ONCE);

        if (!channel.isActive()
                || msg.getPacketQueue().isEmpty()
                    || !attr.compareAndSet(null, true)) {
            out.release();
            return;
        }

        encoder.encodePackets(msg.getPacketQueue(), out, ctx.alloc());
        sendMessage(msg, channel, out);
    }

    private void sendMessage(HttpMessage msg, Channel channel, ByteBuf out) {
        HttpResponse res = createHttpResponse(msg.getOrigin(), out);
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

        ChannelFuture f = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    private HttpResponse createHttpResponse(String origin, ByteBuf message) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        HttpHeaders.addHeader(res, CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpHeaders.addHeader(res, CONNECTION, KEEP_ALIVE);
        if (origin != null) {
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
        HttpHeaders.setContentLength(res, message.readableBytes());

        return res;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof BaseMessage)) {
            super.write(ctx, msg, promise);
            return;
        }

        ByteBuf out = encoder.allocateBuffer(ctx.alloc());

        if (msg instanceof AuthorizeMessage) {
            handle((AuthorizeMessage) msg, ctx.channel(), out);
        }

        if (msg instanceof XHRSendPacketsMessage) {
            write((XHRSendPacketsMessage) msg, ctx, out);
        }
        if (msg instanceof XHROutMessage) {
            sendMessage((XHROutMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof XHRErrorMessage) {
            XHRErrorMessage xhrErrorMessage = (XHRErrorMessage) msg;
            encoder.encodePacket(xhrErrorMessage.getPacket(), out);
            sendMessage(xhrErrorMessage, ctx.channel(), out);
        }

        if (msg instanceof WebSocketPacketMessage) {
            handle((WebSocketPacketMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof WebsocketErrorMessage) {
            handle((WebsocketErrorMessage) msg, ctx.channel(), out);
        }
    }

    private void handle(AuthorizeMessage authMsg, Channel channel, ByteBuf out) throws IOException {
        String message = authMsg.getMsg();
        if (authMsg.getJsonpParam() != null) {
            encoder.encodeJsonP(authMsg.getJsonpParam(), message, out);
        } else {
            out.writeBytes(message.getBytes());
        }
        sendMessage(authMsg, channel, out);
    }

    private void handle(WebSocketPacketMessage webSocketPacketMessage, Channel channel, ByteBuf out) throws IOException {
        encoder.encodePacket(webSocketPacketMessage.getPacket(), out);
        WebSocketFrame res = new TextWebSocketFrame(out);
        log.trace("Out message: {} sessionId: {}",
                        out.toString(CharsetUtil.UTF_8), webSocketPacketMessage.getSessionId());
        channel.writeAndFlush(res);
        if (!out.isReadable()) {
            out.release();
        }
    }

    private void handle(WebsocketErrorMessage websocketErrorMessage, Channel channel, ByteBuf out) throws IOException {
        encoder.encodePacket(websocketErrorMessage.getPacket(), out);
        TextWebSocketFrame frame = new TextWebSocketFrame(out);
        channel.writeAndFlush(frame);
    }

}
