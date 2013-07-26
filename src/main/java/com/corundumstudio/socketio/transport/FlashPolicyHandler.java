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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelHandler.Sharable;

import com.corundumstudio.socketio.SocketIOPipelineFactory;

@Sharable
public class FlashPolicyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ByteBuf requestBuffer = Unpooled.copiedBuffer("<policy-file-request/>", CharsetUtil.UTF_8);
    private final ByteBuf responseBuffer = Unpooled.copiedBuffer(
                            "<?xml version=\"1.0\"?>"
                            + "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">"
                            + "<cross-domain-policy> "
                            + "   <site-control permitted-cross-domain-policies=\"master-only\"/>"
                            + "   <allow-access-from domain=\"*\" to-ports=\"*\" />"
                            + "</cross-domain-policy>", CharsetUtil.UTF_8);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf data = msg.slice(0, requestBuffer.readableBytes());
        if (data.equals(requestBuffer)) {
            ChannelFuture f = ctx.write(responseBuffer);
            f.addListener(ChannelFutureListener.CLOSE);
            return;
        }
        ctx.pipeline().remove(SocketIOPipelineFactory.FLASH_POLICY_HANDLER);
        ctx.fireChannelRead(msg);
    }

}
