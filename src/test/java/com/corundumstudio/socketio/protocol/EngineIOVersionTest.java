package com.corundumstudio.socketio.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for EngineIOVersion enum
 */
public class EngineIOVersionTest extends BaseProtocolTest {

    @Test
    public void testVersionValues() {
        // Test all version values
        assertEquals("2", EngineIOVersion.V2.getValue());
        assertEquals("3", EngineIOVersion.V3.getValue());
        assertEquals("4", EngineIOVersion.V4.getValue());
        assertEquals("", EngineIOVersion.UNKNOWN.getValue());
    }

    @Test
    public void testFromValueWithValidVersions() {
        // Test fromValue with valid version strings
        assertEquals(EngineIOVersion.V2, EngineIOVersion.fromValue("2"));
        assertEquals(EngineIOVersion.V3, EngineIOVersion.fromValue("3"));
        assertEquals(EngineIOVersion.V4, EngineIOVersion.fromValue("4"));
    }

    @Test
    public void testFromValueWithInvalidVersions() {
        // Test fromValue with invalid version strings
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue("1"));
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue("5"));
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue("invalid"));
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue(""));
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue(null));
    }

    @Test
    public void testFromValueWithCaseSensitivity() {
        // Test fromValue is case sensitive
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue("V2"));
        assertEquals(EngineIOVersion.UNKNOWN, EngineIOVersion.fromValue("v2"));
    }

    @Test
    public void testEIOConstant() {
        // Test EIO constant
        assertEquals("EIO", EngineIOVersion.EIO);
    }

    @Test
    public void testVersionMapping() {
        // Test that all versions are properly mapped
        assertNotNull(EngineIOVersion.fromValue("2"));
        assertNotNull(EngineIOVersion.fromValue("3"));
        assertNotNull(EngineIOVersion.fromValue("4"));
        
        // Verify the mapping is consistent
        assertSame(EngineIOVersion.V2, EngineIOVersion.fromValue("2"));
        assertSame(EngineIOVersion.V3, EngineIOVersion.fromValue("3"));
        assertSame(EngineIOVersion.V4, EngineIOVersion.fromValue("4"));
    }

    @Test
    public void testVersionComparison() {
        // Test version comparison logic if needed
        assertNotEquals(EngineIOVersion.V2, EngineIOVersion.V3);
        assertNotEquals(EngineIOVersion.V3, EngineIOVersion.V4);
        assertNotEquals(EngineIOVersion.V2, EngineIOVersion.V4);
    }

    @Test
    public void testUnknownVersionBehavior() {
        // Test UNKNOWN version behavior
        EngineIOVersion unknown = EngineIOVersion.fromValue("999");
        assertEquals(EngineIOVersion.UNKNOWN, unknown);
        assertEquals("", unknown.getValue());
    }

    @Test
    public void testVersionStringRepresentation() {
        // Test string representation of versions
        assertTrue(EngineIOVersion.V2.getValue().matches("\\d+"));
        assertTrue(EngineIOVersion.V3.getValue().matches("\\d+"));
        assertTrue(EngineIOVersion.V4.getValue().matches("\\d+"));
        assertTrue(EngineIOVersion.UNKNOWN.getValue().isEmpty());
    }

    @Test
    public void testVersionUniqueness() {
        // Test that all versions have unique values
        assertNotEquals(EngineIOVersion.V2.getValue(), EngineIOVersion.V3.getValue());
        assertNotEquals(EngineIOVersion.V3.getValue(), EngineIOVersion.V4.getValue());
        assertNotEquals(EngineIOVersion.V2.getValue(), EngineIOVersion.V4.getValue());
    }
}
