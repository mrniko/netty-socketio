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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import mockit.Mocked;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.transport.NamespaceClient;
import com.corundumstudio.socketio.transport.XHRPollingClient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.PacketHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.JacksonJsonSupport;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.transport.MainBaseClient;

public class PacketHandlerTest {

    private Configuration cfg = new Configuration();
    private JsonSupport map = new JacksonJsonSupport(cfg);
    private Decoder decoder = new Decoder(map, new AckManager(null));
    private Encoder encoder = new Encoder(cfg, map);
    private NamespacesHub namespacesHub = new NamespacesHub(cfg);
    @Mocked
    private Channel channel;
    private MainBaseClient client = new XHRPollingClient(null, null, UUID.randomUUID(), null, new MemoryStoreFactory(), null);
    private final AtomicInteger invocations = new AtomicInteger();

    @Before
    public void before() {
        if (namespacesHub.get(Namespace.DEFAULT_NAME) == null) {
            Namespace ns = namespacesHub.create(Namespace.DEFAULT_NAME);
            client.addChildClient(ns);
        }

        invocations.set(0);
    }

    private PacketListener createTestListener(final List<Packet> packets) {
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, NamespaceClient client) {
                int index = invocations.incrementAndGet();
                Packet currentPacket = packets.get(index-1);
                Assert.assertEquals(currentPacket.getType(), packet.getType());
                Assert.assertEquals(currentPacket.getData(), packet.getData());
            }
        };
        return listener;
    }

    @Test
    public void testOnePacket() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    @Test
    public void testUTF8MultiplePackets() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet3 = new Packet(PacketType.CONNECT);
        packets.add(packet3);

        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "Данные"));
        packets.add(packet);

        Packet packet1 = new Packet(PacketType.JSON);
        packet1.setData(Collections.singletonMap("При\ufffdвет", "wq\ufffdeq"));
        packets.add(packet1);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    @Test
    public void testMultiplePackets() throws Exception {
        List<Packet> packets = new ArrayList<Packet>();
        Packet packet3 = new Packet(PacketType.CONNECT);
        packets.add(packet3);

        Packet packet = new Packet(PacketType.JSON);
        packet.setData(Collections.singletonMap("test1", "test2"));
        packets.add(packet);

        Packet packet1 = new Packet(PacketType.JSON);
        packet1.setData(Collections.singletonMap("fsdfdf", "wqeq"));
        packets.add(packet1);

        PacketListener listener = createTestListener(packets);
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        testHandler(handler, new ConcurrentLinkedQueue<Packet>(packets));
    }

    private void testHandler(PacketHandler handler, Queue<Packet> packets) throws Exception {
        int size = packets.size();
        ByteBuf buffer = Unpooled.buffer();
        encoder.encodePackets(packets, buffer, UnpooledByteBufAllocator.DEFAULT);
        handler.channelRead0(null, new PacketsMessage(client, buffer));
        Assert.assertEquals(size, invocations.get());
    }

    //@Test
    public void testDecodePerf() throws Exception {
        PacketListener listener = new PacketListener(null, null, null) {
            @Override
            public void onPacket(Packet packet, NamespaceClient client) {
            }
        };
        PacketHandler handler = new PacketHandler(listener, decoder, namespacesHub);
        long start = System.currentTimeMillis();
        ByteBuf buffer = Unpooled.wrappedBuffer("\ufffd10\ufffd3:::Привет\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::".getBytes());
        for (int i = 0; i < 50000; i++) {
            handler.channelRead0(null, new PacketsMessage(client, buffer));
            buffer.readerIndex(0);
        }
        long end = System.currentTimeMillis() - start;
        System.out.println(end + "ms");
        // 670ms
    }


}
