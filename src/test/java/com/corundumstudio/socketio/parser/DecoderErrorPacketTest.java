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

import org.junit.Assert;
import org.junit.Test;

import com.corundumstudio.socketio.protocol.ErrorAdvice;
import com.corundumstudio.socketio.protocol.ErrorReason;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

public class DecoderErrorPacketTest extends DecoderBaseTest {

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePacket("7:::", null);
        Assert.assertEquals(PacketType.ERROR, packet.getType());
    }

    @Test
    public void testDecodeWithReason() throws IOException {
        Packet packet = decoder.decodePacket("7:::0", null);
        Assert.assertEquals(PacketType.ERROR, packet.getType());
        Assert.assertEquals(ErrorReason.TRANSPORT_NOT_SUPPORTED, packet.getReason());
    }

    @Test
    public void testDecodeWithReasonAndAdvice() throws IOException {
        Packet packet = decoder.decodePacket("7:::2+0", null);
        Assert.assertEquals(PacketType.ERROR, packet.getType());
        Assert.assertEquals(ErrorReason.UNAUTHORIZED, packet.getReason());
        Assert.assertEquals(ErrorAdvice.RECONNECT, packet.getAdvice());
    }

    @Test
    public void testDecodeWithEndpoint() throws IOException {
        Packet packet = decoder.decodePacket("7::/woot", null);
        Assert.assertEquals(PacketType.ERROR, packet.getType());
        Assert.assertEquals("/woot", packet.getNsp());
    }

}
