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

// TODO rewrite to MessageToByteEncoder
@Sharable
public class SocketIOEncoder extends ChannelOutboundHandlerAdapter implements MessageHandler, Disconnectable {

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
            return !prevVal.equals(channel)
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
            Channel channel) throws IOException {
        if (!channel.isActive() || clientEntry.getPackets().isEmpty()
                    || !clientEntry.tryToWrite(channel)) {
            return;
        }

        ByteBuf message = encoder.encodePackets(clientEntry.getPackets());
        sendMessage(origin, sessionId, channel, message);
    }

    private void sendMessage(String origin, UUID sessionId, Channel channel,
            ByteBuf message) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
        addHeaders(origin, res);
        HttpHeaders.setContentLength(res, message.readableBytes());

        if (log.isTraceEnabled()) {
            log.trace("Out message: {} - sessionId: {}",
                        new Object[] { message.toString(CharsetUtil.UTF_8), sessionId });
        }
        ChannelFuture f = channel.write(res);
        channel.write(message);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    private void addHeaders(String origin, HttpResponse res) {
        HttpHeaders.addHeader(res, CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpHeaders.addHeader(res, CONNECTION, KEEP_ALIVE);
        if (origin != null) {
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            HttpHeaders.addHeader(res, ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BaseMessage) {
            BaseMessage message = (BaseMessage) msg;
            message.handleMessage(this, ctx.channel());
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void handle(XHRNewChannelMessage xhrNewChannelMessage, Channel channel) throws IOException {
        XHRClientEntry clientEntry = getXHRClientEntry(channel, xhrNewChannelMessage.getSessionId());

        write(xhrNewChannelMessage.getSessionId(), xhrNewChannelMessage.getOrigin(), clientEntry, channel);
    }

    @Override
    public void handle(XHRPacketMessage xhrPacketMessage, Channel channel) throws IOException {
        XHRClientEntry clientEntry = getXHRClientEntry(channel, xhrPacketMessage.getSessionId());
        clientEntry.addPacket(xhrPacketMessage.getPacket());

        write(xhrPacketMessage.getSessionId(), xhrPacketMessage.getOrigin(), clientEntry, channel);
    }

    @Override
    public void handle(XHROutMessage xhrPostMessage, Channel channel) {
        sendMessage(xhrPostMessage.getOrigin(), null, channel, channel.alloc().buffer(0, 0));
    }

    @Override
    public void handle(AuthorizeMessage authMsg, Channel channel) throws IOException {
        ByteBuf msg;
        String message = authMsg.getMsg();
        if (authMsg.getJsonpParam() != null) {
            msg = encoder.encodeJsonP(authMsg.getJsonpParam(), message);
        } else {
            msg = Unpooled.wrappedBuffer(message.getBytes());
        }
        sendMessage(authMsg.getOrigin(), authMsg.getSessionId(), channel, msg);
    }

    @Override
    public void handle(WebSocketPacketMessage webSocketPacketMessage, Channel channel) throws IOException {
        ByteBuf message = encoder.encodePacket(webSocketPacketMessage.getPacket());
        WebSocketFrame res = new TextWebSocketFrame(message);
        log.trace("Out message: {} sessionId: {}", new Object[] {
                message.toString(CharsetUtil.UTF_8), webSocketPacketMessage.getSessionId()});
        if (channel.isOpen()) {
            channel.write(res);
        } else {
            log.debug("Channel was closed, for sessionId: {}", webSocketPacketMessage.getSessionId());
        }
    }

    @Override
    public void handle(WebsocketErrorMessage websocketErrorMessage, Channel channel) throws IOException {
        ByteBuf message = encoder.encodePacket(websocketErrorMessage.getPacket());
        TextWebSocketFrame frame = new TextWebSocketFrame(message);
        channel.write(frame);
    }

    @Override
    public void handle(XHRErrorMessage xhrErrorMessage, Channel channel) throws IOException {
        ByteBuf message = encoder.encodePacket(xhrErrorMessage.getPacket());
        sendMessage(xhrErrorMessage.getOrigin(), null, channel, message);
    }

    @Override
    public void onDisconnect(BaseClient client) {
        sessionId2ActiveChannelId.remove(client.getSessionId());
    }

}
