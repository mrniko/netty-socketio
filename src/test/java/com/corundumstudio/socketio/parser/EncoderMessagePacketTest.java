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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

public class EncoderMessagePacketTest extends EncoderBaseTest {

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setData("woot");
        ByteBuf result = Unpooled.buffer();
//        encoder.encodePacket(packet, result);
        Assert.assertEquals("3:::woot", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testEncodeWithIdAndEndpoint() throws IOException {
        Packet packet = new Packet(PacketType.MESSAGE);
//        packet.setId(5L);
//        packet.setAck(true);
        packet.setNsp("/tobi");
        ByteBuf result = Unpooled.buffer();
//        encoder.encodePacket(packet, result);
        Assert.assertEquals("3:5:/tobi", result.toString(CharsetUtil.UTF_8));
    }

}
