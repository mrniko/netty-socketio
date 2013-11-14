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

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Sharable
public class ResourceHandler extends ChunkedWriteHandler {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    private final Map<String, String> resources = new HashMap<String, String>();

    public ResourceHandler(String context) {
        addResource(context + "/static/flashsocket/WebSocketMain.swf",
                        "/static/flashsocket/WebSocketMain.swf");
        addResource(context + "/static/flashsocket/WebSocketMainInsecure.swf",
                        "/static/flashsocket/WebSocketMainInsecure.swf");
    }

    public void addResource(String pathPart, String resourcePath) {
    	resources.put(pathPart, resourcePath);
    }

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		if (msg instanceof FullHttpRequest) {
			FullHttpRequest req = (FullHttpRequest) msg;
			QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
			String resource = resources.get(queryDecoder.path());
			if (resource != null) {
				// create ok response
				HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
				// set content type
				HttpHeaders.setHeader(res, HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
				// write header
				ctx.write(res);
				// create resource inputstream and check
				InputStream is = getClass().getResourceAsStream(resource);
				if (is == null) {
					sendError(ctx, NOT_FOUND);
					return;
				}
				// write the stream
				ChannelFuture writeFuture = ctx.channel().write(new ChunkedStream(is));
				// close the channel on finish
				writeFuture.addListener(ChannelFutureListener.CLOSE);

				return;
			}
		}
		ctx.fireChannelRead(msg);
	}

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        
    	HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        HttpHeaders.setHeader(response, CONTENT_TYPE, "text/plain; charset=UTF-8");
        ByteBuf content = Unpooled.copiedBuffer( "Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8);
        // write response
        ctx.channel().write(response);
        // Close the connection as soon as the error message is sent.
        ctx.channel().write(content).addListener(ChannelFutureListener.CLOSE);
    }
}
