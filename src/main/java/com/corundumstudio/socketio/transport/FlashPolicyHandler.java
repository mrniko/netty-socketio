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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;

import com.corundumstudio.socketio.SocketIOPipelineFactory;

@Sharable
public class FlashPolicyHandler extends SimpleChannelUpstreamHandler {

    private final ChannelBuffer requestBuffer = ChannelBuffers.copiedBuffer("<policy-file-request/>", CharsetUtil.UTF_8);
    private final ChannelBuffer responseBuffer = ChannelBuffers.copiedBuffer(
                            "<?xml version=\"1.0\"?>"
                            + "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">"
                            + "<cross-domain-policy> "
                            + "   <site-control permitted-cross-domain-policies=\"master-only\"/>"
                            + "   <allow-access-from domain=\"*\" to-ports=\"*\" />"
                            + "</cross-domain-policy>", CharsetUtil.UTF_8);


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ChannelBuffer inBuffer = (ChannelBuffer) e.getMessage();
        ChannelBuffer data = inBuffer.slice(0, requestBuffer.readableBytes());
        if (data.equals(requestBuffer)) {
            ChannelFuture f = e.getChannel().write(responseBuffer);
            f.addListener(ChannelFutureListener.CLOSE);
            return;
        }
        ctx.getPipeline().remove(SocketIOPipelineFactory.FLASH_POLICY_HANDLER);
        super.messageReceived(ctx, e);
    }
}
