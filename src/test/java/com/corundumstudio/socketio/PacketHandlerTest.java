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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import mockit.Mocked;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.junit.Test;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class PacketHandlerTest {

    private ObjectMapper map = new ObjectMapper();
    private Decoder decoder = new Decoder(map);
    private Encoder encoder = new Encoder(map);
    @Mocked
    private Channel channel;
    @Mocked
    private SocketIOClient client;

    @Test
    public void testOnePacket() throws Exception {
        final AtomicInteger invocations = new AtomicInteger();
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, SocketIOClient client) {
                invocations.incrementAndGet();
                Assert.assertEquals(PacketType.JSON, packet.getType());
                Map<String, String> map = (Map<String, String>) packet.getData();
                Assert.assertTrue(map.keySet().size() == 1);
                Assert.assertTrue(map.keySet().contains("test1"));
            }
        };
        PacketHandler handler = new PacketHandler(listener, decoder);

        List<Packet> packets = new ArrayList<Packet>();
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        testHandler(invocations, handler, packets);
    }

    @Test
    public void testMultiplePackets() throws Exception {
        final AtomicInteger invocations = new AtomicInteger();
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, SocketIOClient client) {
                if (packet.getType() == PacketType.CONNECT) {
                    invocations.incrementAndGet();
                    return;
                }
                Assert.assertEquals(PacketType.JSON, packet.getType());
                Map<String, String> map = (Map<String, String>) packet.getData();
                Set<String> keys = new HashSet<String>();
                keys.add("test1");
                keys.add("fsdfdf");
                Assert.assertTrue(map.keySet().size() == 1);
                Assert.assertTrue(map.keySet().removeAll(keys));
                invocations.incrementAndGet();
            }
        };
        PacketHandler handler = new PacketHandler(listener, decoder);

        List<Packet> packets = new ArrayList<Packet>();
        Packet packet3 = new Packet(PacketType.CONNECT);
        packets.add(packet3);

        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        Packet packet1 = new Packet(PacketType.JSON);
        packet1.setData(Collections.singletonMap("fsdfdf", "wqeq"));
        packets.add(packet1);

        testHandler(invocations, handler, packets);
    }

    private void testHandler(final AtomicInteger invocations,
            PacketHandler handler, List<Packet> packets) throws Exception {
        String str = encoder.encodePackets(packets);
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(str.getBytes());
        handler.messageReceived(null, new UpstreamMessageEvent(channel, new PacketsMessage(client, buffer), null));
        Assert.assertEquals(packets.size(), invocations.get());
    }

    //@Test
    public void testDecodePerf() throws Exception {
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, SocketIOClient client) {
            }
        };
        PacketHandler handler = new PacketHandler(listener, decoder);
        long start = System.currentTimeMillis();
        ChannelBuffer buffer = ChannelBuffers.wrappedBuffer("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::".getBytes());
        for (int i = 0; i < 50000; i++) {
            ChannelBuffer t = buffer.copy();
            handler.messageReceived(null, new UpstreamMessageEvent(channel, new PacketsMessage(client, t), null));
        }
        long end = System.currentTimeMillis() - start;
        System.out.println(end + "ms");
        // 1143ms
    }


}
