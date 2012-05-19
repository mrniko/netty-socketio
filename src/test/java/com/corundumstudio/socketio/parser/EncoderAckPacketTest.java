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
import java.util.Arrays;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Assert;
import org.junit.Test;

public class EncoderAckPacketTest {

    private Encoder encoder = new Encoder(new ObjectMapper());

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.ACK);
        packet.setAckId("140");
        ChannelBuffer result = encoder.encodePacket(packet);
        Assert.assertEquals("6:::140", result.toString(CharsetUtil.UTF_8));
    }

    @Test
    public void testEncodeWithArgs() throws IOException {
        Packet packet = new Packet(PacketType.ACK);
        packet.setAckId("12");
        packet.setArgs(Arrays.asList("woot", "wa"));
        ChannelBuffer result = encoder.encodePacket(packet);
        Assert.assertEquals("6:::12+[\"woot\",\"wa\"]", result.toString(CharsetUtil.UTF_8));
    }

}
