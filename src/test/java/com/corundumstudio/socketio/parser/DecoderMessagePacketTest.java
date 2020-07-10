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

import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

@Ignore
public class DecoderMessagePacketTest extends DecoderBaseTest {

    @Test
    public void testDecodeId() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("3:1::asdfasdf", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.MESSAGE, packet.getType());
//        Assert.assertEquals(1, (long)packet.getId());
//        Assert.assertTrue(packet.getArgs().isEmpty());
//        Assert.assertTrue(packet.getAck().equals(Boolean.TRUE));
    }

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("3:::woot", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.MESSAGE, packet.getType());
        Assert.assertEquals("woot", packet.getData());
    }

    @Test
    public void testDecodeWithIdAndEndpoint() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("3:5:/tobi", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.MESSAGE, packet.getType());
//        Assert.assertEquals(5, (long)packet.getId());
//        Assert.assertEquals(true, packet.getAck());
        Assert.assertEquals("/tobi", packet.getNsp());
    }

}
