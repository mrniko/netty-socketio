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

import io.netty.buffer.ByteBuf;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.PacketListener;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.BaseClient;
import com.corundumstudio.socketio.transport.NamespaceClient;

@Sharable
public class PacketHandler extends SimpleChannelInboundHandler<PacketsMessage> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PacketListener packetListener;
    private final Decoder decoder;
    private final NamespacesHub namespacesHub;

    public PacketHandler(PacketListener packetListener, Decoder decoder, NamespacesHub namespacesHub) {
        super();
        this.packetListener = packetListener;
        this.decoder = decoder;
        this.namespacesHub = namespacesHub;
    }

    @Override
    protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, PacketsMessage message)
                throws Exception {
        ByteBuf content = message.getContent();
        BaseClient client = message.getClient();

        if (log.isTraceEnabled()) {
            log.trace("In message: {} sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }
        while (content.isReadable()) {
            try {
                Packet packet = decoder.decodePackets(content, client.getSessionId());
                Namespace ns = namespacesHub.get(packet.getEndpoint());
                if (ns == null) {
                    log.warn("Can't find namespace for endpoint: {} probably it was removed.", packet.getEndpoint());
                    return;
                }

                NamespaceClient nClient = (NamespaceClient) client.getChildClient(ns);
                packetListener.onPacket(packet, nClient);
            } catch (Exception ex) {
                String c = content.toString(CharsetUtil.UTF_8);
                log.error("Error during data processing. Client sessionId: " + client.getSessionId() + ", data: " + c, ex);
                return;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        log.error("Exception occurs", e);
    }

}
