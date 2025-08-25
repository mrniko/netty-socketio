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
package com.corundumstudio.socketio.handler;

import java.util.Queue;

import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Utility class for testing client packet sending behavior.
 *
 * This class provides common assertion methods for verifying that clients
 * correctly send packets through the client.send() method. These utilities
 * are designed to be used across different handler tests to ensure consistent
 * verification of packet sending behavior.
 *
 * Key Features:
 * - Verifies that client.send() was called by checking packet queues
 * - Validates packet format and content
 * - Supports different packet types and verification scenarios
 * - Provides reusable assertions for integration tests
 *
 * Usage Example:
 * <pre>
 * ClientHead client = createTestClient();
 * // ... trigger some handler logic that should send a packet
 *
 * ClientPacketTestUtils.assertClientSentPacket(client, PacketType.MESSAGE, PacketType.ERROR);
 * ClientPacketTestUtils.assertErrorPacketSent(client, "/invalid_namespace", "Invalid namespace");
 * </pre>
 */
public class ClientPacketTestUtils {

    /**
     * Asserts that a client has sent at least one packet.
     *
     * This method verifies that the client.send() method was called by checking
     * that the client's packet queue for the current transport is not empty.
     *
     * @param client The ClientHead instance to check
     * @throws AssertionError if no packets were sent
     */
    private static void assertClientSentPacket(ClientHead client) {
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        assertFalse(packetQueue.isEmpty(), "Client should have sent at least one packet");
    }

    /**
     * Asserts that a client has sent a packet with the specified type and subtype.
     *
     * This method verifies that:
     * 1. The client sent at least one packet
     * 2. The first packet in the queue has the expected type and subtype
     *
     * @param client The ClientHead instance to check
     * @param expectedType The expected packet type
     * @param expectedSubType The expected packet subtype (can be null)
     * @throws AssertionError if the packet doesn't match expectations
     */
    private static void assertClientSentPacket(ClientHead client, PacketType expectedType, PacketType expectedSubType) {
        // Verify that at least one packet was sent
        assertClientSentPacket(client);

        // Get the packet and verify its format
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        Packet packet = packetQueue.peek(); // Don't remove, just peek

        assertNotNull(packet, "Packet should not be null");
        assertEquals(expectedType, packet.getType(), "Packet type should match expected");

        if (expectedSubType != null) {
            assertEquals(expectedSubType, packet.getSubType(), "Packet subtype should match expected");
        }
    }

    /**
     * Asserts that a client has sent an error packet with specific details.
     *
     * This method is specifically designed for verifying error packets that
     * contain namespace and error message information, such as those sent
     * when invalid namespaces are accessed.
     *
     * @param client The ClientHead instance to check
     * @param expectedNamespace The expected namespace in the error packet
     * @param expectedErrorMessage The expected error message
     * @throws AssertionError if the error packet doesn't match expectations
     */
    public static void assertErrorPacketSent(ClientHead client, String expectedNamespace, String expectedErrorMessage) {
        // Verify the basic packet structure
        assertClientSentPacket(client, PacketType.MESSAGE, PacketType.ERROR);

        // Get the packet and verify error-specific details
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        Packet errorPacket = packetQueue.peek();

        assertEquals(expectedNamespace, errorPacket.getNsp(), "Error packet namespace should match expected");
        assertEquals(expectedErrorMessage, errorPacket.getData(), "Error packet message should match expected");
    }

    /**
     * Asserts that a client has sent an OPEN packet with session information.
     *
     * This method is specifically designed for verifying OPEN packets that
     * are sent during client authorization and connection establishment.
     *
     * @param client The ClientHead instance to check
     * @throws AssertionError if the OPEN packet is not found or incorrect
     */
    public static void assertOpenPacketSent(ClientHead client) {
        // Verify that an OPEN packet was sent
        assertClientSentPacket(client, PacketType.OPEN, null);

        // Get the packet and verify OPEN-specific details
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        Packet openPacket = packetQueue.peek();

        assertNotNull(openPacket.getData(), "OPEN packet should contain data");
    }

    /**
     * Gets the first packet from the client's queue without removing it.
     *
     * This utility method allows for more detailed inspection of packets
     * when the standard assertion methods are not sufficient.
     *
     * @param client The ClientHead instance to check
     * @return The first packet in the queue, or null if queue is empty
     */
    public static Packet peekFirstPacket(ClientHead client) {
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        return packetQueue.peek();
    }

    /**
     * Gets the number of packets in the client's queue.
     *
     * This utility method allows for verification of the exact number
     * of packets sent by the client.
     *
     * @param client The ClientHead instance to check
     * @return The number of packets in the client's queue
     */
    public static int getPacketCount(ClientHead client) {
        Queue<Packet> packetQueue = client.getPacketsQueue(client.getCurrentTransport());
        return packetQueue.size();
    }
}
