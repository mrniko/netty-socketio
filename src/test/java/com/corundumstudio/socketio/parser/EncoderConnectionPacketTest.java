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

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EncoderConnectionPacketTest {

    private Encoder encoder = new Encoder(new ObjectMapper());

    @Test
    public void testEncodeHeartbeat() throws IOException {
        Packet packet = new Packet(PacketType.HEARTBEAT);
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("2::", result);
    }

    @Test
    public void testEncodeDisconnection() throws IOException {
        Packet packet = new Packet(PacketType.DISCONNECT);
        packet.setEndpoint("/woot");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("0::/woot", result);
    }

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.CONNECT);
        packet.setEndpoint("/tobi");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("1::/tobi", result);
    }

    @Test
    public void testEncodePacketWithQueryString() throws IOException {
        Packet packet = new Packet(PacketType.CONNECT);
        packet.setEndpoint("/test");
        packet.setQs("?test=1");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("1::/test:?test=1", result);
    }

}
