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
package com.zizzle.socketio.parser;

import java.io.IOException;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import mockit.Expectations;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.zizzle.socketio.AckCallback;
import com.zizzle.socketio.protocol.Packet;
import com.zizzle.socketio.protocol.PacketType;
import com.fasterxml.jackson.core.JsonParseException;

@Ignore
public class DecoderAckPacketTest extends DecoderBaseTest {

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("6:::140", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.ACK, packet.getType());
        Assert.assertEquals(140, (long)packet.getAckId());
//        Assert.assertTrue(packet.getArgs().isEmpty());
    }

    @Test
    public void testDecodeWithArgs() throws IOException {
        initExpectations();

        Packet packet = decoder.decodePackets(Unpooled.copiedBuffer("6:::12+[\"woot\",\"wa\"]", CharsetUtil.UTF_8), null);
        Assert.assertEquals(PacketType.ACK, packet.getType());
        Assert.assertEquals(12, (long)packet.getAckId());
//        Assert.assertEquals(Arrays.<Object>asList("woot", "wa"), packet.getArgs());
    }

    private void initExpectations() {
        new Expectations() {{
            ackManager.getCallback((UUID)any, anyInt);
            result = new AckCallback<String>(String.class) {
                @Override
                public void onSuccess(String result) {
                }
            };
        }};
    }

    @Test(expected = JsonParseException.class)
    public void testDecodeWithBadJson() throws IOException {
        initExpectations();
        decoder.decodePackets(Unpooled.copiedBuffer("6:::1+{\"++]", CharsetUtil.UTF_8), null);
    }

}
