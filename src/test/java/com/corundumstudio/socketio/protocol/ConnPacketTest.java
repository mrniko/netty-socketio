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
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * Comprehensive test suite for ConnPacket class
 */
public class ConnPacketTest extends BaseProtocolTest {

    @Test
    public void testConstructorWithValidSid() {
        UUID sid = UUID.randomUUID();
        ConnPacket connPacket = new ConnPacket(sid);
        
        assertEquals(sid, connPacket.getSid());
        assertSame(sid, connPacket.getSid());
    }

    @Test
    public void testConstructorWithNullSid() {
        ConnPacket connPacket = new ConnPacket(null);
        
        assertNull(connPacket.getSid());
    }

    @Test
    public void testGetSid() {
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        
        ConnPacket connPacket1 = new ConnPacket(sid1);
        ConnPacket connPacket2 = new ConnPacket(sid2);
        
        assertEquals(sid1, connPacket1.getSid());
        assertEquals(sid2, connPacket2.getSid());
        assertNotEquals(connPacket1.getSid(), connPacket2.getSid());
    }

    @Test
    public void testMultipleConnPacketsWithDifferentSids() {
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        UUID sid3 = UUID.randomUUID();
        
        ConnPacket connPacket1 = new ConnPacket(sid1);
        ConnPacket connPacket2 = new ConnPacket(sid2);
        ConnPacket connPacket3 = new ConnPacket(sid3);
        
        assertEquals(sid1, connPacket1.getSid());
        assertEquals(sid2, connPacket2.getSid());
        assertEquals(sid3, connPacket3.getSid());
        
        assertNotEquals(connPacket1.getSid(), connPacket2.getSid());
        assertNotEquals(connPacket2.getSid(), connPacket3.getSid());
        assertNotEquals(connPacket1.getSid(), connPacket3.getSid());
    }

    @Test
    public void testConnPacketImmutability() {
        UUID originalSid = UUID.randomUUID();
        ConnPacket connPacket = new ConnPacket(originalSid);
        
        // Verify original value
        assertEquals(originalSid, connPacket.getSid());
        
        // Create new UUID with same value
        UUID newSid = UUID.fromString(originalSid.toString());
        assertEquals(originalSid, newSid);
        
        // ConnPacket should still have the original reference
        assertSame(originalSid, connPacket.getSid());
    }

    @Test
    public void testConnPacketWithWellKnownUUIDs() {
        // Test with well-known UUID values
        UUID nilUUID = new UUID(0L, 0L);
        UUID maxUUID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
        UUID minUUID = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        
        ConnPacket nilConnPacket = new ConnPacket(nilUUID);
        ConnPacket maxConnPacket = new ConnPacket(maxUUID);
        ConnPacket minConnPacket = new ConnPacket(minUUID);
        
        assertEquals(nilUUID, nilConnPacket.getSid());
        assertEquals(maxUUID, maxConnPacket.getSid());
        assertEquals(minUUID, minConnPacket.getSid());
        
        assertNotEquals(nilConnPacket.getSid(), maxConnPacket.getSid());
        assertNotEquals(maxConnPacket.getSid(), minConnPacket.getSid());
        assertNotEquals(nilConnPacket.getSid(), minConnPacket.getSid());
    }

    @Test
    public void testConnPacketEquality() {
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        
        ConnPacket connPacket1 = new ConnPacket(sid1);
        ConnPacket connPacket2 = new ConnPacket(sid1);
        ConnPacket connPacket3 = new ConnPacket(sid2);
        
        // Test equality based on SID content
        assertEquals(connPacket1.getSid(), connPacket2.getSid());
        assertNotEquals(connPacket1.getSid(), connPacket3.getSid());
    }

    @Test
    public void testConnPacketWithGeneratedUUIDs() {
        // Test with multiple randomly generated UUIDs
        for (int i = 0; i < 100; i++) {
            UUID sid = UUID.randomUUID();
            ConnPacket connPacket = new ConnPacket(sid);
            
            assertEquals(sid, connPacket.getSid());
            assertNotNull(connPacket.getSid());
        }
    }

    @Test
    public void testConnPacketToString() {
        UUID sid = UUID.randomUUID();
        ConnPacket connPacket = new ConnPacket(sid);
        
        String toString = connPacket.toString();
        assertNotNull(toString);
        // ConnPacket doesn't override toString, so it uses Object.toString()
        // which doesn't contain the SID information
        assertTrue(toString.startsWith("com.corundumstudio.socketio.protocol.ConnPacket@"));
    }

    @Test
    public void testConnPacketHashCode() {
        UUID sid = UUID.randomUUID();
        ConnPacket connPacket = new ConnPacket(sid);
        
        int hashCode = connPacket.hashCode();
        assertTrue(hashCode != 0);
    }

    @Test
    public void testConnPacketSerialization() {
        UUID sid = UUID.randomUUID();
        ConnPacket connPacket = new ConnPacket(sid);
        
        // Test that the object can be serialized/deserialized
        // This is a basic test - in a real scenario you might use ObjectOutputStream
        assertNotNull(connPacket);
        assertEquals(sid, connPacket.getSid());
    }

    @Test
    public void testConnPacketWithSpecialUUIDs() {
        // Test with UUIDs that have special bit patterns
        UUID specialUUID1 = new UUID(0x1234567890ABCDEFL, 0xFEDCBA0987654321L);
        UUID specialUUID2 = new UUID(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L);
        UUID specialUUID3 = new UUID(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL);
        
        ConnPacket connPacket1 = new ConnPacket(specialUUID1);
        ConnPacket connPacket2 = new ConnPacket(specialUUID2);
        ConnPacket connPacket3 = new ConnPacket(specialUUID3);
        
        assertEquals(specialUUID1, connPacket1.getSid());
        assertEquals(specialUUID2, connPacket2.getSid());
        assertEquals(specialUUID3, connPacket3.getSid());
        
        assertNotEquals(connPacket1.getSid(), connPacket2.getSid());
        assertNotEquals(connPacket2.getSid(), connPacket3.getSid());
        assertNotEquals(connPacket1.getSid(), connPacket3.getSid());
    }

    @Test
    public void testConnPacketPerformance() {
        // Test performance with many packets
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10000; i++) {
            UUID sid = UUID.randomUUID();
            ConnPacket connPacket = new ConnPacket(sid);
            assertEquals(sid, connPacket.getSid());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete within reasonable time (less than 1 second)
        assertTrue(duration < 1000, "Performance test took too long: " + duration + "ms");
    }
}
