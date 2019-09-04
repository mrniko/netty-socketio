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
package com.corundumstudio.socketio.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PacketTest {

    @Test
    public void packetCopyIsCreatedWhenNamespaceDiffers() {
        Packet oldPacket = createPacket();

        String newNs = "new";
        Packet newPacket = oldPacket.withNsp(newNs);
        assertEquals(newNs, newPacket.getNsp());
        assertPacketCopied(oldPacket, newPacket);
    }

    @Test
    public void packetCopyIsCreatedWhenNewNamespaceDiffersAndIsNull() {
        Packet packet = createPacket();
        Packet newPacket = packet.withNsp(null);
        assertNull(newPacket.getNsp());
        assertPacketCopied(packet, newPacket);
    }

    @Test
    public void originalPacketReturnedIfNamespaceIsTheSame() {
        Packet packet = new Packet(PacketType.MESSAGE);
        assertSame(packet, packet.withNsp(""));
    }

    private void assertPacketCopied(Packet oldPacket, Packet newPacket) {
        assertNotSame(newPacket, oldPacket);
        assertEquals(oldPacket.getName(), newPacket.getName());
        assertEquals(oldPacket.getType(), newPacket.getType());
        assertEquals(oldPacket.getSubType(), newPacket.getSubType());
        assertEquals(oldPacket.getAckId(), newPacket.getAckId());
        assertEquals(oldPacket.getAttachments().size(), newPacket.getAttachments().size());
        assertSame(oldPacket.getAttachments(), newPacket.getAttachments());
        assertEquals(oldPacket.getData(), newPacket.getData());
        assertSame(oldPacket.getDataSource(), newPacket.getDataSource());
    }

    private Packet createPacket() {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName("packetName");
        packet.setData("data");
        packet.setAckId(1L);
        packet.setNsp("old");
        packet.setDataSource(Unpooled.wrappedBuffer(new byte[]{10}));
        packet.initAttachments(1);
        packet.addAttachment(Unpooled.wrappedBuffer(new byte[]{20}));
        return packet;
    }
}