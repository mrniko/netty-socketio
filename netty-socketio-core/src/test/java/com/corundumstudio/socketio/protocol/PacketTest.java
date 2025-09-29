/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;

/**
 * Comprehensive test suite for Packet class
 */
public class PacketTest extends BaseProtocolTest {

    @Test
    public void packetCopyIsCreatedWhenNamespaceDiffers() {
        Packet oldPacket = createPacket();

        String newNs = "new";
        Packet newPacket = oldPacket.withNsp(newNs, EngineIOVersion.UNKNOWN);
        assertEquals(newNs, newPacket.getNsp());
        assertPacketCopied(oldPacket, newPacket);
    }

    @Test
    public void packetCopyIsCreatedWhenNewNamespaceDiffersAndIsNull() {
        Packet packet = createPacket();
        Packet newPacket = packet.withNsp(null, EngineIOVersion.UNKNOWN);
        assertNull(newPacket.getNsp());
    }

    @Test
    public void originalPacketReturnedIfNamespaceIsTheSame() {
        Packet packet = new Packet(PacketType.MESSAGE);
        assertSame(packet, packet.withNsp("", EngineIOVersion.UNKNOWN));
    }

    @Test
    public void testPacketConstructorWithType() {
        Packet packet = new Packet(PacketType.EVENT);
        assertEquals(PacketType.EVENT, packet.getType());
        assertNull(packet.getSubType());
        assertNull(packet.getAckId());
        assertNull(packet.getName());
        assertEquals("", packet.getNsp());
        assertNull(packet.getData());
    }

    @Test
    public void testPacketConstructorWithTypeAndEngineIOVersion() {
        Packet packet = new Packet(PacketType.EVENT, EngineIOVersion.V4);
        assertEquals(PacketType.EVENT, packet.getType());
        assertEquals(EngineIOVersion.V4, packet.getEngineIOVersion());
    }

    @Test
    public void testGetType() {
        Packet packet = new Packet(PacketType.MESSAGE);
        assertEquals(PacketType.MESSAGE, packet.getType());
    }

    @Test
    public void testSetAndGetSubType() {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        assertEquals(PacketType.EVENT, packet.getSubType());
    }

    @Test
    public void testSetAndGetName() {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setName("testEvent");
        assertEquals("testEvent", packet.getName());
    }

    @Test
    public void testSetAndGetData() {
        Packet packet = new Packet(PacketType.MESSAGE);
        String testData = "testData";
        packet.setData(testData);
        assertEquals(testData, packet.getData());
    }

    @Test
    public void testSetAndGetAckId() {
        Packet packet = new Packet(PacketType.MESSAGE);
        Long ackId = 123L;
        packet.setAckId(ackId);
        assertEquals(ackId, packet.getAckId());
    }

    @Test
    public void testIsAckRequested() {
        Packet packet = new Packet(PacketType.MESSAGE);
        assertFalse(packet.isAckRequested());
        
        packet.setAckId(123L);
        assertTrue(packet.isAckRequested());
        
        packet.setAckId(null);
        assertFalse(packet.isAckRequested());
    }

    @Test
    public void testSetAndGetNsp() {
        Packet packet = new Packet(PacketType.MESSAGE);
        
        // Test normal namespace
        packet.setNsp("/admin");
        assertEquals("/admin", packet.getNsp());
        
        // Test special case with "{}"
        packet.setNsp("{}");
        assertEquals("", packet.getNsp());
        
        // Test empty namespace
        packet.setNsp("");
        assertEquals("", packet.getNsp());
    }

    @Test
    public void testAttachments() {
        Packet packet = new Packet(PacketType.MESSAGE);
        
        // Test initial state
        assertFalse(packet.hasAttachments());
        assertTrue(packet.getAttachments().isEmpty());
        assertTrue(packet.isAttachmentsLoaded());
        
        // Test with attachments
        packet.initAttachments(2);
        assertTrue(packet.hasAttachments());
        assertFalse(packet.isAttachmentsLoaded());
        
        io.netty.buffer.ByteBuf attachment1 = Unpooled.wrappedBuffer("attachment1".getBytes());
        io.netty.buffer.ByteBuf attachment2 = Unpooled.wrappedBuffer("attachment2".getBytes());
        
        packet.addAttachment(attachment1);
        packet.addAttachment(attachment2);
        
        assertTrue(packet.isAttachmentsLoaded());
        assertEquals(2, packet.getAttachments().size());
        
        // Test attachment limit
        io.netty.buffer.ByteBuf extraAttachment = Unpooled.wrappedBuffer("extra".getBytes());
        packet.addAttachment(extraAttachment);
        assertEquals(2, packet.getAttachments().size()); // Should not exceed limit
    }

    @Test
    public void testSetAndGetDataSource() {
        Packet packet = new Packet(PacketType.MESSAGE);
        io.netty.buffer.ByteBuf dataSource = Unpooled.wrappedBuffer("source".getBytes());
        
        packet.setDataSource(dataSource);
        assertEquals(dataSource, packet.getDataSource());
    }

    @Test
    public void testSetAndGetEngineIOVersion() {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setEngineIOVersion(EngineIOVersion.V4);
        assertEquals(EngineIOVersion.V4, packet.getEngineIOVersion());
    }

    @Test
    public void testToString() {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setAckId(123L);
        
        String toString = packet.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("EVENT"));
        assertTrue(toString.contains("123"));
    }

    @Test
    public void testPacketWithAllFields() {
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setSubType(PacketType.EVENT);
        packet.setName("testEvent");
        packet.setData("testData");
        packet.setAckId(456L);
        packet.setNsp("/test");
        packet.setDataSource(Unpooled.wrappedBuffer("source".getBytes()));
        packet.initAttachments(1);
        packet.addAttachment(Unpooled.wrappedBuffer("attachment".getBytes()));
        
        assertEquals(PacketType.MESSAGE, packet.getType());
        assertEquals(EngineIOVersion.V4, packet.getEngineIOVersion());
        assertEquals(PacketType.EVENT, packet.getSubType());
        assertEquals("testEvent", packet.getName());
        assertEquals("testData", packet.getData());
        assertEquals(Long.valueOf(456), packet.getAckId());
        assertEquals("/test", packet.getNsp());
        assertNotNull(packet.getDataSource());
        assertTrue(packet.hasAttachments());
        assertTrue(packet.isAttachmentsLoaded());
        assertEquals(1, packet.getAttachments().size());
    }

    @Test
    public void testPacketCopyWithDifferentNamespace() {
        Packet originalPacket = createPacket();
        String newNamespace = "/newNamespace";
        
        Packet copiedPacket = originalPacket.withNsp(newNamespace, EngineIOVersion.V4);
        
        assertEquals(newNamespace, copiedPacket.getNsp());
        assertNotSame(originalPacket, copiedPacket);
        assertEquals(originalPacket.getName(), copiedPacket.getName());
        assertEquals(originalPacket.getType(), copiedPacket.getType());
        assertEquals(originalPacket.getSubType(), copiedPacket.getSubType());
        assertEquals(originalPacket.getAckId(), copiedPacket.getAckId());
        // Use raw type comparison to avoid generic type issues
        Object originalData = originalPacket.getData();
        Object copiedData = copiedPacket.getData();
        assertEquals(originalData, copiedData);
        assertSame(originalPacket.getAttachments(), copiedPacket.getAttachments());
        assertSame(originalPacket.getDataSource(), copiedPacket.getDataSource());
    }

    @Test
    public void testPacketCopyWithSameNamespace() {
        Packet originalPacket = createPacket();
        String sameNamespace = originalPacket.getNsp();
        
        Packet copiedPacket = originalPacket.withNsp(sameNamespace, EngineIOVersion.V4);
        
        assertSame(originalPacket, copiedPacket);
    }

    private void assertPacketCopied(Packet oldPacket, Packet newPacket) {
        assertNotSame(newPacket, oldPacket);
        assertEquals(oldPacket.getName(), newPacket.getName());
        assertEquals(oldPacket.getType(), newPacket.getType());
        assertEquals(oldPacket.getSubType(), newPacket.getSubType());
        assertEquals(oldPacket.getAckId(), newPacket.getAckId());
        assertEquals(oldPacket.getAttachments().size(), newPacket.getAttachments().size());
        assertSame(oldPacket.getAttachments(), newPacket.getAttachments());
        // Use raw type comparison to avoid generic type issues
        Object oldData = oldPacket.getData();
        Object newData = newPacket.getData();
        assertEquals(oldData, newData);
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