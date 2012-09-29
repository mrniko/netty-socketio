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

import java.util.Collections;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Packet;

@Sharable
public class PacketHandler extends SimpleChannelUpstreamHandler {

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
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof PacketsMessage) {
            PacketsMessage message = (PacketsMessage) msg;
            ChannelBuffer content = message.getContent();

            if (log.isTraceEnabled()) {
                log.trace("In message: {} sessionId: {}", new Object[] {content.toString(CharsetUtil.UTF_8), message.getClient().getSessionId()});
            }
            while (content.readable()) {
                Packet packet = decoder.decodePackets(content, message.getClient().getSessionId());
                Namespace ns = namespacesHub.get(packet.getEndpoint());

                SocketIOClient client = message.getClient().getChildClient(ns);
                AckRequest ackSender = new AckRequest(packet, client);
                packetListener.onPacket(packet, client, ackSender);
                ackSender.sendAckData(Collections.emptyList());
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

}
