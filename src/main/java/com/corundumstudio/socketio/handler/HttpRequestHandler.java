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

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.HttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@Sharable
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final Configuration configuration;

    public HttpRequestHandler(Configuration configuration) {
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
            HttpRequestBody body = new HttpRequestBody(req);

            HttpRequestSignature httpRequestSignature = new HttpRequestSignature(method, path);
            for (HttpRequestListener httpRequestListener : configuration.getHttpRequestListeners()) {
                if (httpRequestSignature.equals(httpRequestListener.signature())) {
                    try {
                        HttpResponse httpResponse = httpRequestListener.handle(params, headers, body);
                        io.netty.handler.codec.http.HttpResponse res = new DefaultHttpResponse(HTTP_1_1, httpResponse.getHttpResponseStatus());
                        if (httpResponse.getHttpHeaders() != null) {
                            res.headers().add(httpResponse.getHttpHeaders());
                        }
                        if (httpResponse.getBody() != null) {
                            ctx.write(Unpooled.copiedBuffer(httpResponse.getBody(), httpResponse.getCharset()));
                        }
                        channel.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
                        log.debug("Http response, query params: {} headers: {}", params, headers);
                        return;
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        super.channelRead(ctx, msg);
    }
}
