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
package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

public class PayloadTest {

    private final Decoder decoder = new Decoder(new JacksonJsonSupport());
    private final Encoder encoder = new Encoder(new JacksonJsonSupport());

    @Test
    public void testPayloadDecode() throws IOException {
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::".getBytes());
        List<Packet> payload = new ArrayList<Packet>();
        while (buffer.readable()) {
            Packet packet = decoder.decodePackets(buffer);
            payload.add(packet);
        }

        Assert.assertEquals(3, payload.size());
        Packet msg1 = payload.get(0);
        Assert.assertEquals(PacketType.MESSAGE, msg1.getType());
        Assert.assertEquals("5", msg1.getData());
        Packet msg2 = payload.get(1);
        Assert.assertEquals(PacketType.MESSAGE, msg2.getType());
        Assert.assertEquals("53d", msg2.getData());
        Packet msg3 = payload.get(2);
        Assert.assertEquals(PacketType.DISCONNECT, msg3.getType());
    }

    @Test
    public void testPayloadEncode() throws IOException {
        Queue<Packet> packets = new ConcurrentLinkedQueue<Packet>();
        Packet packet1 = new Packet(PacketType.MESSAGE);
        packet1.setData("5");
        packets.add(packet1);

        Packet packet2 = new Packet(PacketType.MESSAGE);
        packet2.setData("53d");
        packets.add(packet2);

        ChannelBuffer result = encoder.encodePackets(packets);
        Assert.assertEquals("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testDecodingNewline() throws IOException {
        Packet packet = decoder.decodePacket("3:::\n");
        Assert.assertEquals(PacketType.MESSAGE, packet.getType());
        Assert.assertEquals("\n", packet.getData());
    }

}
