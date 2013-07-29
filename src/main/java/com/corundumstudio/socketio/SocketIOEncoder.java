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
package com.corundumstudio.socketio;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.AuthorizeMessage;
import com.corundumstudio.socketio.messages.BaseMessage;
import com.corundumstudio.socketio.messages.WebSocketPacketMessage;
import com.corundumstudio.socketio.messages.WebsocketErrorMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHRNewChannelMessage;
import com.corundumstudio.socketio.messages.XHROutMessage;
import com.corundumstudio.socketio.messages.XHRPacketMessage;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.BaseClient;

@Sharable
public class SocketIOEncoder extends ChannelOutboundHandlerAdapter implements Disconnectable {

    class XHRClientEntry {

        // AtomicInteger works faster than locking
        final AtomicReference<Channel> lastChannel = new AtomicReference<Channel>();
        final Queue<Packet> packets = new ConcurrentLinkedQueue<Packet>();

        public void addPacket(Packet packet) {
            packets.add(packet);
        }

        public Queue<Packet> getPackets() {
            return packets;
        }

        /**
         * We can write to channel only once.
         *
         * @param channel
         * @return true - can write
         */
        public boolean tryToWrite(Channel channel) {
            Channel prevVal = lastChannel.get();
            return !channel.equals(prevVal)
                            && lastChannel.compareAndSet(prevVal, channel);
        }

    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<UUID, XHRClientEntry> sessionId2ActiveChannelId = new ConcurrentHashMap<UUID, XHRClientEntry>();
    private final Encoder encoder;

    public SocketIOEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    private XHRClientEntry getXHRClientEntry(Channel channel, UUID sessionId) {
        XHRClientEntry clientEntry = sessionId2ActiveChannelId.get(sessionId);
        if (clientEntry == null) {
            clientEntry = new XHRClientEntry();
            XHRClientEntry old = sessionId2ActiveChannelId.putIfAbsent(sessionId, clientEntry);
            if (old != null) {
                clientEntry = old;
            }
        }
        return clientEntry;
    }

    private void write(UUID sessionId, String origin, XHRClientEntry clientEntry,
            Channel channel, ByteBuf out) throws IOException {
        if (!channel.isActive() || clientEntry.getPackets().isEmpty()
                    || !clientEntry.tryToWrite(channel)) {
            out.release();
            return;
        }

        encoder.encodePackets(clientEntry.getPackets(), out);
        sendMessage(origin, sessionId, channel, out);
    }

    private void sendMessage(String origin, UUID sessionId, Channel channel, ByteBuf out) {
        HttpResponse res = createHttpResponse(origin, out);
        channel.write(res);

        if (out.isReadable()) {
            if (log.isTraceEnabled()) {
                log.trace("Out message: {} - sessionId: {}",
                        new Object[] { out.toString(CharsetUtil.UTF_8), sessionId });
            }
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
        }

//      TODO use it!
//        ByteBuf out = ctx.alloc().ioBuffer();
        ByteBuf out = Unpooled.buffer();

        if (msg instanceof AuthorizeMessage) {
            handle((AuthorizeMessage) msg, ctx.channel(), out);
        }

        if (msg instanceof XHRNewChannelMessage) {
            handle((XHRNewChannelMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof XHRPacketMessage) {
            handle((XHRPacketMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof XHROutMessage) {
            handle((XHROutMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof XHRErrorMessage) {
            handle((XHRErrorMessage) msg, ctx.channel(), out);
        }

        if (msg instanceof WebSocketPacketMessage) {
            handle((WebSocketPacketMessage) msg, ctx.channel(), out);
        }
        if (msg instanceof WebsocketErrorMessage) {
            handle((WebsocketErrorMessage) msg, ctx.channel(), out);
        }
    }

    public void handle(XHRNewChannelMessage xhrNewChannelMessage, Channel channel, ByteBuf out) throws IOException {
        XHRClientEntry clientEntry = getXHRClientEntry(channel, xhrNewChannelMessage.getSessionId());

        write(xhrNewChannelMessage.getSessionId(), xhrNewChannelMessage.getOrigin(), clientEntry, channel, out);
    }

    public void handle(XHRPacketMessage xhrPacketMessage, Channel channel, ByteBuf out) throws IOException {
        XHRClientEntry clientEntry = getXHRClientEntry(channel, xhrPacketMessage.getSessionId());
        clientEntry.addPacket(xhrPacketMessage.getPacket());

        write(xhrPacketMessage.getSessionId(), xhrPacketMessage.getOrigin(), clientEntry, channel, out);
    }

    public void handle(XHROutMessage xhrPostMessage, Channel channel, ByteBuf out) {
        sendMessage(xhrPostMessage.getOrigin(), xhrPostMessage.getSessionId(), channel, out);
    }

    public void handle(AuthorizeMessage authMsg, Channel channel, ByteBuf out) throws IOException {
        String message = authMsg.getMsg();
        if (authMsg.getJsonpParam() != null) {
            encoder.encodeJsonP(authMsg.getJsonpParam(), message, out);
        } else {
            out.writeBytes(message.getBytes());
        }
        sendMessage(authMsg.getOrigin(), authMsg.getSessionId(), channel, out);
    }

    public void handle(XHRErrorMessage xhrErrorMessage, Channel channel, ByteBuf out) throws IOException {
        encoder.encodePacket(xhrErrorMessage.getPacket(), out);
        sendMessage(xhrErrorMessage.getOrigin(), null, channel, out);
    }

    @Override
    public void onDisconnect(BaseClient client) {
        sessionId2ActiveChannelId.remove(client.getSessionId());
    }

    public void handle(WebSocketPacketMessage webSocketPacketMessage, Channel channel, ByteBuf out) throws IOException {
        encoder.encodePacket(webSocketPacketMessage.getPacket(), out);
        WebSocketFrame res = new TextWebSocketFrame(out);
        log.trace("Out message: {} sessionId: {}", new Object[] {
                out.toString(CharsetUtil.UTF_8), webSocketPacketMessage.getSessionId()});
        channel.write(res);
        if (!out.isReadable()) {
            out.release();
        }
    }

    public void handle(WebsocketErrorMessage websocketErrorMessage, Channel channel, ByteBuf out) throws IOException {
        encoder.encodePacket(websocketErrorMessage.getPacket(), out);
        TextWebSocketFrame frame = new TextWebSocketFrame(out);
        channel.write(frame);
    }

}
