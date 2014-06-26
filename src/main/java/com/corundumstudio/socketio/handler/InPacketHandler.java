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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.PacketDecoder;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.transport.NamespaceClient;

@Sharable
public class InPacketHandler extends SimpleChannelInboundHandler<PacketsMessage> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PacketListener packetListener;
    private final PacketDecoder decoder;
    private final NamespacesHub namespacesHub;
    private final ExceptionListener exceptionListener;

    public InPacketHandler(PacketListener packetListener, PacketDecoder decoder, NamespacesHub namespacesHub, ExceptionListener exceptionListener) {
        super();
        this.packetListener = packetListener;
        this.decoder = decoder;
        this.namespacesHub = namespacesHub;
        this.exceptionListener = exceptionListener;
    }

    @Override
    protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, PacketsMessage message)
                throws Exception {
        ByteBuf content = message.getContent();
        ClientHead client = message.getClient();

        if (log.isTraceEnabled()) {
            log.trace("In message: {} sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }
        while (content.isReadable()) {
            try {
                Packet packet = decoder.decodePackets(content, client.getSessionId());
                Namespace ns = namespacesHub.get(packet.getNsp());
                if (ns == null) {
                    log.debug("Can't find namespace for endpoint: {}, sessionId: {} probably it was removed.", packet.getNsp(), client.getSessionId());
                    return;
                }

                if (packet.getSubType() == PacketType.CONNECT) {
                    client.addNamespaceClient(ns);
                }

                NamespaceClient nClient = client.getChildClient(ns);
                if (nClient == null) {
                    log.debug("Can't find namespace client in namespace: {}, sessionId: {} probably it was disconnected.", ns.getName(), client.getSessionId());
                    return;
                }
                packetListener.onPacket(packet, nClient, message.getTransport());
            } catch (Exception ex) {
                String c = content.toString(CharsetUtil.UTF_8);
                log.error("Error during data processing. Client sessionId: " + client.getSessionId() + ", data: " + c, ex);
                return;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        if (!exceptionListener.exceptionCaught(ctx, e)) {
            super.exceptionCaught(ctx, e);
        }
    }

}
