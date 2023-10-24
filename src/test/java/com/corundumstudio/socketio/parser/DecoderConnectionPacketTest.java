/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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
public class DecoderConnectionPacketTest extends DecoderBaseTest {

    @Test
    public void testDecodeHeartbeat() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("2:::", CharsetUtil.UTF_8), null);
//        Assert.assertEquals(PacketType.HEARTBEAT, packet.getType());
    }

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("1::/tobi", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.CONNECT, packet.getType());
        Assert.assertEquals("/tobi", packet.getNsp());
    }

    @Test
    public void testDecodeWithQueryString() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("1::/test:?test=1", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.CONNECT, packet.getType());
        Assert.assertEquals("/test", packet.getNsp());
//        Assert.assertEquals("?test=1", packet.getQs());
    }

    @Test
    public void testDecodeDisconnection() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("0::/woot", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.DISCONNECT, packet.getType());
        Assert.assertEquals("/woot", packet.getNsp());
    }

}
