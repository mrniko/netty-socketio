/**
 * Copyright (c) 2012-2019 Nikita Koksharov
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
import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.corundumstudio.socketio.protocol.Packet;

@Ignore
public class DecoderJsonPacketTest extends DecoderBaseTest {

    @Test
    public void testUTF8Decode() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("4:::\"Привет\"", CharsetUtil.UTF_8), null);
//        Assert.assertEquals(PacketType.JSON, packet.getType());
        Assert.assertEquals("Привет", packet.getData());
    }

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("4:::\"2\"", CharsetUtil.UTF_8), null);
//        Assert.assertEquals(PacketType.JSON, packet.getType());
        Assert.assertEquals("2", packet.getData());
    }

    @Test
    public void testDecodeWithMessageIdAndAckData() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("4:1+::{\"a\":\"b\"}", CharsetUtil.UTF_8), null);
//        Assert.assertEquals(PacketType.JSON, packet.getType());
//        Assert.assertEquals(1, (long)packet.getId());
//        Assert.assertEquals(Packet.ACK_DATA, packet.getAck());

        Map obj = (Map) packet.getData();
        Assert.assertEquals("b", obj.get("a"));
        Assert.assertEquals(1, obj.size());
    }

}
