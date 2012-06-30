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
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

public class EncoderJsonPacketTest extends EncoderBaseTest {

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData("2");
        ChannelBuffer result = encoder.encodePacket(packet);
        Assert.assertEquals("4:::\"2\"", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testEncodeWithMessageIdAndAckData() throws IOException {
        Packet packet = new Packet(PacketType.JSON);
        packet.setId(1L);
        packet.setAck("data");
        packet.setData(Collections.singletonMap("a", "b"));
        ChannelBuffer result = encoder.encodePacket(packet);
        Assert.assertEquals("4:1+::{\"a\":\"b\"}", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testPerf() throws IOException {
        List<Packet> packets = new ArrayList<Packet>();
        for (int i = 0; i < 100; i++) {
            Packet packet = new Packet(PacketType.JSON);
            packet.setId(1L);
            packet.setData(Collections.singletonMap("Привет", "123123jksdf213"));
            packets.add(packet);
        }

        List<Queue<Packet>> queues = new ArrayList<Queue<Packet>>();
        for (int i = 0; i < 5000; i++) {
            ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue(packets);
            queues.add(queue);

        }
        long t = System.currentTimeMillis();

        for (int i = 0; i < 5000; i++) {
            encoder.encodePackets(queues.get(i));
//            String message = encoder.encodePackets(queues.get(i));
//            ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8);
        }
        // 1000 ms

        System.out.println(System.currentTimeMillis() - t);
    }

}
