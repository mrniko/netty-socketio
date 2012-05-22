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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

@Sharable
public class PacketHandler extends SimpleChannelUpstreamHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PacketListener packetListener;
    private final Decoder decoder;

    public PacketHandler(PacketListener packetListener, Decoder decoder) {
        super();
        this.packetListener = packetListener;
        this.decoder = decoder;
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
                Packet packet = decoder.decodePackets(content);
                sendAck(packet, message.getClient());
                packetListener.onPacket(packet, message.getClient());
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

    private void sendAck(Packet packet, SocketIOClient client) {
        if (packet.getId() != null &&
                !"data".equals(packet.getAck())) {
            Packet ackPacket = new Packet(PacketType.ACK);
            ackPacket.setAckId(packet.getId());
            ackPacket.setEndpoint(packet.getEndpoint());
            client.send(ackPacket);
        }
    }

}
