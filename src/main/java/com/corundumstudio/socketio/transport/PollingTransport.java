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

import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.handler.ClientsBox;
import com.corundumstudio.socketio.handler.EncoderHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.protocol.PacketDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class PollingTransport extends ChannelInboundHandlerAdapter {

    public static final String NAME = "polling";

    private static final Logger log = LoggerFactory.getLogger(PollingTransport.class);

    private final PacketDecoder decoder;
    private final ClientsBox clientsBox;
    private final AuthorizeHandler authorizeHandler;

    public PollingTransport(PacketDecoder decoder, AuthorizeHandler authorizeHandler, ClientsBox clientsBox) {
        this.decoder = decoder;
        this.authorizeHandler = authorizeHandler;
        this.clientsBox = clientsBox;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());

            List<String> transport = queryDecoder.parameters().get("transport");
            
            if (transport != null && NAME.equals(transport.get(0))) {
                List<String> sid = queryDecoder.parameters().get("sid");
                List<String> j = queryDecoder.parameters().get("j");
                List<String> b64 = queryDecoder.parameters().get("b64");

                String origin = req.headers().get(HttpHeaderNames.ORIGIN);
                ctx.channel().attr(EncoderHandler.ORIGIN).set(origin);

                String userAgent = req.headers().get(HttpHeaderNames.USER_AGENT);
                ctx.channel().attr(EncoderHandler.USER_AGENT).set(userAgent);

                if (j != null && j.get(0) != null) {
                    Integer index = Integer.valueOf(j.get(0));
                    ctx.channel().attr(EncoderHandler.JSONP_INDEX).set(index);
                }
                if (b64 != null && b64.get(0) != null) {
                    Integer enable = Integer.valueOf(b64.get(0));
                    ctx.channel().attr(EncoderHandler.B64).set(enable == 1);
                }

                try {
                    if (sid != null && sid.get(0) != null) {
                        final Long sessionId = Long.valueOf(sid.get(0));
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

    private void handleMessage(FullHttpRequest req, Long sessionId, QueryStringDecoder queryDecoder, ChannelHandlerContext ctx)
                                                                                throws IOException {
            String origin = req.headers().get(HttpHeaderNames.ORIGIN);
            if (queryDecoder.parameters().containsKey("disconnect")) {
                ClientHead client = clientsBox.get(sessionId);
                client.onChannelDisconnect();
                ctx.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));
            } else if (HttpMethod.POST.equals(req.method())) {
                onPost(sessionId, ctx, origin, req.content());
            } else if (HttpMethod.GET.equals(req.method())) {
                onGet(sessionId, ctx, origin);
            } else if (HttpMethod.OPTIONS.equals(req.method())) {
                onOptions(sessionId, ctx, origin);
            } else {
                log.error("Wrong {} method invocation for {}", req.method(), sessionId);
                sendError(ctx);
            }
    }

    private void onOptions(Long sessionId, ChannelHandlerContext ctx, String origin) {
        ClientHead client = clientsBox.get(sessionId);
        if (client == null) {
            log.error("{} is not registered. Closing connection", sessionId);
            sendError(ctx);
            return;
        }

        ctx.channel().writeAndFlush(new XHROptionsMessage(origin, sessionId));
    }

    private void onPost(Long sessionId, ChannelHandlerContext ctx, String origin, ByteBuf content)
                                                                                throws IOException {
        ClientHead client = clientsBox.get(sessionId);
        if (client == null) {
            log.error("{} is not registered. Closing connection", sessionId);
            sendError(ctx);
            return;
        }


        // release POST response before message processing
        ctx.channel().writeAndFlush(new XHRPostMessage(origin, sessionId));

        Boolean b64 = ctx.channel().attr(EncoderHandler.B64).get();
        if (b64 != null && b64) {
            Integer jsonIndex = ctx.channel().attr(EncoderHandler.JSONP_INDEX).get();
            content = decoder.preprocessJson(jsonIndex, content);
        }

        ctx.pipeline().fireChannelRead(new PacketsMessage(client, content, Transport.POLLING));
    }

    protected void onGet(Long sessionId, ChannelHandlerContext ctx, String origin) {
        ClientHead client = clientsBox.get(sessionId);
        if (client == null) {
            log.error("{} is not registered. Closing connection", sessionId);
            sendError(ctx);
            return;
        }

        client.bindChannel(ctx.channel(), Transport.POLLING);

        authorizeHandler.connect(client);
    }

    private void sendError(ChannelHandlerContext ctx) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        ctx.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }

}
