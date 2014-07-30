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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import com.corundumstudio.socketio.Configuration;

@Sharable
public class FlashPolicyHandler extends ChannelInboundHandlerAdapter {

    private final ByteBuf requestBuffer = Unpooled.copiedBuffer( "<policy-file-request/>", CharsetUtil.UTF_8);
    private ByteBuf responseBuffer;

    public FlashPolicyHandler(Configuration configuration) {
        try {
            if (configuration.getCrossDomainPolicy() == null) {
                URL resUrl = getClass().getResource("/static/flashsocket/crossdomain.xml");
                URLConnection urlConnection = resUrl.openConnection();

                InputStream stream = urlConnection.getInputStream();
                try {
                    readFile(stream);
                } finally {
                    stream.close();
                }
            } else {
                readFile(configuration.getCrossDomainPolicy());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void readFile(InputStream stream) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(stream);
        ByteBuffer buffer = ByteBuffer.allocate(5*1024);
        channel.read(buffer);
        buffer.flip();
        responseBuffer = Unpooled.copiedBuffer(buffer);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf message = (ByteBuf) msg;
            ByteBuf data = message.slice(0, requestBuffer.readableBytes());
            if (data.equals(requestBuffer)) {
                message.release();
                ChannelFuture f = ctx.writeAndFlush(Unpooled.copiedBuffer(responseBuffer));
                f.addListener(ChannelFutureListener.CLOSE);
                return;
            }
            ctx.pipeline().remove(this);
        }
        ctx.fireChannelRead(msg);
    }

}
