package com.corundumstudio.socketio.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for PacketType enum
 */
public class PacketTypeTest extends BaseProtocolTest {

    @Test
    public void testEngineIOPacketTypes() {
        // Test Engine.IO packet types (non-inner)
        assertEquals(0, PacketType.OPEN.getValue());
        assertEquals(1, PacketType.CLOSE.getValue());
        assertEquals(2, PacketType.PING.getValue());
        assertEquals(3, PacketType.PONG.getValue());
        assertEquals(4, PacketType.MESSAGE.getValue());
        assertEquals(5, PacketType.UPGRADE.getValue());
        assertEquals(6, PacketType.NOOP.getValue());
        
        // Verify these are not inner types by testing valueOf behavior
        assertEquals(PacketType.OPEN, PacketType.valueOf(0));
        assertEquals(PacketType.CLOSE, PacketType.valueOf(1));
        assertEquals(PacketType.PING, PacketType.valueOf(2));
        assertEquals(PacketType.PONG, PacketType.valueOf(3));
        assertEquals(PacketType.MESSAGE, PacketType.valueOf(4));
        assertEquals(PacketType.UPGRADE, PacketType.valueOf(5));
        assertEquals(PacketType.NOOP, PacketType.valueOf(6));
    }

    @Test
    public void testSocketIOPacketTypes() {
        // Test Socket.IO packet types (inner)
        assertEquals(0, PacketType.CONNECT.getValue());
        assertEquals(1, PacketType.DISCONNECT.getValue());
        assertEquals(2, PacketType.EVENT.getValue());
        assertEquals(3, PacketType.ACK.getValue());
        assertEquals(4, PacketType.ERROR.getValue());
        assertEquals(5, PacketType.BINARY_EVENT.getValue());
        assertEquals(6, PacketType.BINARY_ACK.getValue());
        
        // Verify these are inner types by testing valueOfInner behavior
        assertEquals(PacketType.CONNECT, PacketType.valueOfInner(0));
        assertEquals(PacketType.DISCONNECT, PacketType.valueOfInner(1));
        assertEquals(PacketType.EVENT, PacketType.valueOfInner(2));
        assertEquals(PacketType.ACK, PacketType.valueOfInner(3));
        assertEquals(PacketType.ERROR, PacketType.valueOfInner(4));
        assertEquals(PacketType.BINARY_EVENT, PacketType.valueOfInner(5));
        assertEquals(PacketType.BINARY_ACK, PacketType.valueOfInner(6));
    }

    @Test
    public void testValueOfWithEngineIOTypes() {
        // Test valueOf for Engine.IO types
        assertEquals(PacketType.OPEN, PacketType.valueOf(0));
        assertEquals(PacketType.CLOSE, PacketType.valueOf(1));
        assertEquals(PacketType.PING, PacketType.valueOf(2));
        assertEquals(PacketType.PONG, PacketType.valueOf(3));
        assertEquals(PacketType.MESSAGE, PacketType.valueOf(4));
        assertEquals(PacketType.UPGRADE, PacketType.valueOf(5));
        assertEquals(PacketType.NOOP, PacketType.valueOf(6));
    }

    @Test
    public void testValueOfWithSocketIOTypesShouldReturnEngineIOTypes() {
        // Test valueOf for Socket.IO types should return Engine.IO types
        // OPEN(0) is not inner, so valueOf(0) should return OPEN, not CONNECT
        assertEquals(PacketType.OPEN, PacketType.valueOf(0));
        assertEquals(PacketType.CLOSE, PacketType.valueOf(1));
        assertEquals(PacketType.PING, PacketType.valueOf(2));
        assertEquals(PacketType.PONG, PacketType.valueOf(3));
        assertEquals(PacketType.MESSAGE, PacketType.valueOf(4));
        assertEquals(PacketType.UPGRADE, PacketType.valueOf(5));
        assertEquals(PacketType.NOOP, PacketType.valueOf(6));
    }

    @Test
    public void testValueOfInnerWithSocketIOTypes() {
        // Test valueOfInner for Socket.IO types
        assertEquals(PacketType.CONNECT, PacketType.valueOfInner(0));
        assertEquals(PacketType.DISCONNECT, PacketType.valueOfInner(1));
        assertEquals(PacketType.EVENT, PacketType.valueOfInner(2));
        assertEquals(PacketType.ACK, PacketType.valueOfInner(3));
        assertEquals(PacketType.ERROR, PacketType.valueOfInner(4));
        assertEquals(PacketType.BINARY_EVENT, PacketType.valueOfInner(5));
        assertEquals(PacketType.BINARY_ACK, PacketType.valueOfInner(6));
    }

    @Test
    public void testValueOfInnerWithEngineIOTypesShouldReturnSocketIOTypes() {
        // Test valueOfInner for Engine.IO types should return Socket.IO types
        // ERROR(4, true) is inner type, so valueOfInner(4) should return ERROR, not MESSAGE
        assertEquals(PacketType.ERROR, PacketType.valueOfInner(4));
        
        // Test that valueOfInner works for all Socket.IO types
        assertEquals(PacketType.CONNECT, PacketType.valueOfInner(0));
        assertEquals(PacketType.DISCONNECT, PacketType.valueOfInner(1));
        assertEquals(PacketType.EVENT, PacketType.valueOfInner(2));
        assertEquals(PacketType.ACK, PacketType.valueOfInner(3));
        assertEquals(PacketType.BINARY_EVENT, PacketType.valueOfInner(5));
        assertEquals(PacketType.BINARY_ACK, PacketType.valueOfInner(6));
    }

    @Test
    public void testValueOfInnerWithInvalidValueShouldThrowException() {
        // Test valueOfInner with invalid value
        try {
            PacketType.valueOfInner(99);
            fail("Expected IllegalArgumentException for invalid value");
        } catch (IllegalArgumentException e) {
            assertEquals("Can't parse 99", e.getMessage());
        }
    }

    @Test
    public void testValuesArray() {
        // Test that VALUES array contains all enum values
        PacketType[] values = PacketType.VALUES;
        assertEquals(14, values.length); // 7 Engine.IO + 7 Socket.IO types
        
        // Verify all values are present
        assertTrue(contains(values, PacketType.OPEN));
        assertTrue(contains(values, PacketType.CONNECT));
        assertTrue(contains(values, PacketType.BINARY_ACK));
    }

    @Test
    public void testGetValue() {
        // Test getValue method for all types
        for (PacketType type : PacketType.VALUES) {
            assertNotNull(type.getValue());
            assertTrue(type.getValue() >= 0);
            assertTrue(type.getValue() <= 6);
        }
    }

    @Test
    public void testInnerFlagBehavior() {
        // Test inner flag behavior through public methods
        // OPEN and MESSAGE should work with valueOf (not inner)
        assertEquals(PacketType.OPEN, PacketType.valueOf(0));
        assertEquals(PacketType.MESSAGE, PacketType.valueOf(4));
        
        // CONNECT and EVENT should work with valueOfInner (inner)
        assertEquals(PacketType.CONNECT, PacketType.valueOfInner(0));
        assertEquals(PacketType.EVENT, PacketType.valueOfInner(2));
    }

    private boolean contains(PacketType[] array, PacketType value) {
        for (PacketType type : array) {
            if (type == value) {
                return true;
            }
        }
        return false;
    }
}
