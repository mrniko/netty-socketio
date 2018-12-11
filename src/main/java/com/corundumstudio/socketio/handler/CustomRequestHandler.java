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

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.CustomRequestListener;
import com.corundumstudio.socketio.RequestBody;
import com.corundumstudio.socketio.RequestSignature;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class CustomRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(CustomRequestHandler.class);

    private final Configuration configuration;

    public CustomRequestHandler(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            Channel channel = ctx.channel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());

            HttpMethod method = req.method();
            String path = queryDecoder.path();
            Map<String, List<String>> params = queryDecoder.parameters();
            HttpHeaders headers = req.headers();
            RequestBody body = new RequestBody(req);

            RequestSignature requestSignature = new RequestSignature(method, path);
            HttpResponse res = null;
            for (CustomRequestListener customRequestListener : configuration.getCustomRequestListeners()) {
                if (requestSignature.equals(customRequestListener.signature())) {
                    try {
                        res = customRequestListener.handle(params, headers, body);
                    } catch (Exception ignore) {
                    }
                    // process one request only
                    if (res != null) break;
                }
            }
            if (res != null) {
                channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
                req.release();
                log.warn("Blocked wrong socket.io-context request! url: {}, params: {}, ip: {}", queryDecoder.path(), queryDecoder.parameters(), channel.remoteAddress());
                return;
            }
        }
        super.channelRead(ctx, msg);
    }
}
