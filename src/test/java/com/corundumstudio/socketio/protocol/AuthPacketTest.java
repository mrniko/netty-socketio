package com.corundumstudio.socketio.protocol;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.UUID;

/**
 * Comprehensive test suite for AuthPacket class
 */
public class AuthPacketTest extends BaseProtocolTest {

    @Test
    public void testConstructorWithValidParameters() {
        UUID sid = UUID.randomUUID();
        String[] upgrades = {"websocket", "polling"};
        int pingInterval = 25000;
        int pingTimeout = 5000;
        
        AuthPacket authPacket = new AuthPacket(sid, upgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgrades, authPacket.getUpgrades());
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithEmptyUpgrades() {
        UUID sid = UUID.randomUUID();
        String[] emptyUpgrades = {};
        int pingInterval = 30000;
        int pingTimeout = 6000;
        
        AuthPacket authPacket = new AuthPacket(sid, emptyUpgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(emptyUpgrades, authPacket.getUpgrades());
        assertEquals(0, authPacket.getUpgrades().length);
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithNullUpgrades() {
        UUID sid = UUID.randomUUID();
        String[] nullUpgrades = null;
        int pingInterval = 20000;
        int pingTimeout = 4000;
        
        AuthPacket authPacket = new AuthPacket(sid, nullUpgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertNull(authPacket.getUpgrades());
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithSingleUpgrade() {
        UUID sid = UUID.randomUUID();
        String[] singleUpgrade = {"websocket"};
        int pingInterval = 15000;
        int pingTimeout = 3000;
        
        AuthPacket authPacket = new AuthPacket(sid, singleUpgrade, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(singleUpgrade, authPacket.getUpgrades());
        assertEquals(1, authPacket.getUpgrades().length);
        assertEquals("websocket", authPacket.getUpgrades()[0]);
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithMultipleUpgrades() {
        UUID sid = UUID.randomUUID();
        String[] multipleUpgrades = {"websocket", "polling", "flashsocket", "xhr-polling"};
        int pingInterval = 35000;
        int pingTimeout = 7000;
        
        AuthPacket authPacket = new AuthPacket(sid, multipleUpgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(multipleUpgrades, authPacket.getUpgrades());
        assertEquals(4, authPacket.getUpgrades().length);
        assertEquals("websocket", authPacket.getUpgrades()[0]);
        assertEquals("polling", authPacket.getUpgrades()[1]);
        assertEquals("flashsocket", authPacket.getUpgrades()[2]);
        assertEquals("xhr-polling", authPacket.getUpgrades()[3]);
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithZeroValues() {
        UUID sid = UUID.randomUUID();
        String[] upgrades = {"websocket"};
        int pingInterval = 0;
        int pingTimeout = 0;
        
        AuthPacket authPacket = new AuthPacket(sid, upgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgrades, authPacket.getUpgrades());
        assertEquals(0, authPacket.getPingInterval());
        assertEquals(0, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithNegativeValues() {
        UUID sid = UUID.randomUUID();
        String[] upgrades = {"websocket"};
        int pingInterval = -1000;
        int pingTimeout = -500;
        
        AuthPacket authPacket = new AuthPacket(sid, upgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgrades, authPacket.getUpgrades());
        assertEquals(-1000, authPacket.getPingInterval());
        assertEquals(-500, authPacket.getPingTimeout());
    }

    @Test
    public void testConstructorWithLargeValues() {
        UUID sid = UUID.randomUUID();
        String[] upgrades = {"websocket"};
        int pingInterval = Integer.MAX_VALUE;
        int pingTimeout = Integer.MAX_VALUE;
        
        AuthPacket authPacket = new AuthPacket(sid, upgrades, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgrades, authPacket.getUpgrades());
        assertEquals(Integer.MAX_VALUE, authPacket.getPingInterval());
        assertEquals(Integer.MAX_VALUE, authPacket.getPingTimeout());
    }

    @Test
    public void testGetSid() {
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        
        AuthPacket authPacket1 = new AuthPacket(sid1, new String[]{"websocket"}, 25000, 5000);
        AuthPacket authPacket2 = new AuthPacket(sid2, new String[]{"polling"}, 30000, 6000);
        
        assertEquals(sid1, authPacket1.getSid());
        assertEquals(sid2, authPacket2.getSid());
        assertNotEquals(authPacket1.getSid(), authPacket2.getSid());
    }

    @Test
    public void testGetUpgrades() {
        String[] upgrades1 = {"websocket", "polling"};
        String[] upgrades2 = {"flashsocket", "xhr-polling"};
        
        AuthPacket authPacket1 = new AuthPacket(UUID.randomUUID(), upgrades1, 25000, 5000);
        AuthPacket authPacket2 = new AuthPacket(UUID.randomUUID(), upgrades2, 30000, 6000);
        
        assertArrayEquals(upgrades1, authPacket1.getUpgrades());
        assertArrayEquals(upgrades2, authPacket2.getUpgrades());
        assertNotEquals(authPacket1.getUpgrades(), authPacket2.getUpgrades());
    }

    @Test
    public void testGetPingInterval() {
        int pingInterval1 = 25000;
        int pingInterval2 = 30000;
        
        AuthPacket authPacket1 = new AuthPacket(UUID.randomUUID(), new String[]{"websocket"}, pingInterval1, 5000);
        AuthPacket authPacket2 = new AuthPacket(UUID.randomUUID(), new String[]{"polling"}, pingInterval2, 6000);
        
        assertEquals(pingInterval1, authPacket1.getPingInterval());
        assertEquals(pingInterval2, authPacket2.getPingInterval());
        assertNotEquals(authPacket1.getPingInterval(), authPacket2.getPingInterval());
    }

    @Test
    public void testGetPingTimeout() {
        int pingTimeout1 = 5000;
        int pingTimeout2 = 6000;
        
        AuthPacket authPacket1 = new AuthPacket(UUID.randomUUID(), new String[]{"websocket"}, 25000, pingTimeout1);
        AuthPacket authPacket2 = new AuthPacket(UUID.randomUUID(), new String[]{"polling"}, 30000, pingTimeout2);
        
        assertEquals(pingTimeout1, authPacket1.getPingTimeout());
        assertEquals(pingTimeout2, authPacket2.getPingTimeout());
        assertNotEquals(authPacket1.getPingTimeout(), authPacket2.getPingTimeout());
    }

    @Test
    public void testAuthPacketImmutability() {
        UUID sid = UUID.randomUUID();
        String[] originalUpgrades = {"websocket", "polling"};
        int pingInterval = 25000;
        int pingTimeout = 5000;
        
        AuthPacket authPacket = new AuthPacket(sid, originalUpgrades, pingInterval, pingTimeout);
        
        // Verify original values
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(originalUpgrades, authPacket.getUpgrades());
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
        
        // Modify the original arrays
        originalUpgrades[0] = "modified";
        
        // AuthPacket should reflect the changes since it holds a direct reference
        // This is the actual behavior of the AuthPacket class
        assertArrayEquals(new String[]{"modified", "polling"}, authPacket.getUpgrades());
    }

    @Test
    public void testAuthPacketWithSpecialCharacters() {
        UUID sid = UUID.randomUUID();
        String[] upgradesWithSpecialChars = {"websocket!@#", "polling$%^", "flashsocket&*()"};
        int pingInterval = 25000;
        int pingTimeout = 5000;
        
        AuthPacket authPacket = new AuthPacket(sid, upgradesWithSpecialChars, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgradesWithSpecialChars, authPacket.getUpgrades());
        assertEquals(3, authPacket.getUpgrades().length);
        assertEquals("websocket!@#", authPacket.getUpgrades()[0]);
        assertEquals("polling$%^", authPacket.getUpgrades()[1]);
        assertEquals("flashsocket&*()", authPacket.getUpgrades()[2]);
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }

    @Test
    public void testAuthPacketWithUnicodeCharacters() {
        UUID sid = UUID.randomUUID();
        String[] upgradesWithUnicode = {"websocket协议", "polling传输", "flashsocket连接"};
        int pingInterval = 25000;
        int pingTimeout = 5000;
        
        AuthPacket authPacket = new AuthPacket(sid, upgradesWithUnicode, pingInterval, pingTimeout);
        
        assertEquals(sid, authPacket.getSid());
        assertArrayEquals(upgradesWithUnicode, authPacket.getUpgrades());
        assertEquals(3, authPacket.getUpgrades().length);
        assertEquals("websocket协议", authPacket.getUpgrades()[0]);
        assertEquals("polling传输", authPacket.getUpgrades()[1]);
        assertEquals("flashsocket连接", authPacket.getUpgrades()[2]);
        assertEquals(pingInterval, authPacket.getPingInterval());
        assertEquals(pingTimeout, authPacket.getPingTimeout());
    }
}
