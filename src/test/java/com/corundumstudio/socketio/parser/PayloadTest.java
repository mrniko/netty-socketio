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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Assert;
import org.junit.Test;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.protocol.Decoder;
import com.corundumstudio.socketio.protocol.Encoder;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

public class PayloadTest {

    private final JacksonJsonSupport support = new JacksonJsonSupport(new Configuration());
    private final Decoder decoder = new Decoder(support, null);
    private final Encoder encoder = new Encoder(new Configuration(), support);

    @Test
    public void testPayloadDecode() throws IOException {
        ByteBuf buffer = Unpooled.wrappedBuffer("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::".getBytes());
        List<Packet> payload = new ArrayList<Packet>();
        while (buffer.isReadable()) {
            Packet packet = decoder.decodePackets(buffer, null);
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

        ByteBuf result = Unpooled.buffer();
//        encoder.encodePackets(packets, result, UnpooledByteBufAllocator.DEFAULT);
        Assert.assertEquals("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testDecodingNewline() throws IOException {
        Packet packet = decoder.decodePacket("3:::\n", null);
        Assert.assertEquals(PacketType.MESSAGE, packet.getType());
        Assert.assertEquals("\n", packet.getData());
    }

}
