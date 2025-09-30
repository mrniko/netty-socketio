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
package com.corundumstudio.socketio.integration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;

import io.socket.client.Socket;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO room broadcasting functionality.
 * Note: This test is simplified to avoid Kryo serialization issues with Java modules.
 */
@DisplayName("Room Broadcasting Tests - SocketIO Protocol ROOMS & EVENT")
public class RoomBroadcastTest extends AbstractSocketIOIntegrationTest {
    private final String testEvent = faker.app().name();
    private final String testData = faker.address().fullAddress();

    @Test
    @DisplayName("Should broadcast messages to all clients in a specific room")
    public void testBroadcastingToRoom() throws Exception {
        // Test broadcasting messages to specific rooms
        // Note: This test is simplified to avoid Kryo serialization issues with Java modules
        CountDownLatch connectLatch = new CountDownLatch(2);
        AtomicInteger connectedClients = new AtomicInteger(0);
        AtomicReference<Object> receivedData1 = new AtomicReference<>();
        AtomicReference<Object> receivedData2 = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClients.incrementAndGet();
                connectLatch.countDown();
            }
        });

        // Connect two clients
        Socket client1 = createClient();
        Socket client2 = createClient();

        client1.on(testEvent, args -> {
            receivedData1.set(args[0]);
        });
        client2.on(testEvent, args -> {
            receivedData2.set(args[0]);
        });

        client1.connect();
        client2.connect();

        // Wait for both connections
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Both clients should connect within 10 seconds");
        assertEquals(2, connectedClients.get(), "Two clients should be connected");

        // Get server clients
        // Get server clients in a straightforward way
        java.util.List<SocketIOClient> serverClients = new java.util.ArrayList<>(getServer().getAllClients());
        assertEquals(2, serverClients.size(), "There should be exactly two server clients");
        SocketIOClient serverClient1 = serverClients.get(0);
        SocketIOClient serverClient2 = serverClients.get(1);
        assertNotNull(serverClient2, "Second server client should not be null");

        // Join both clients to the same room
        String roomName = generateRoomName("broadcast");
        serverClient1.joinRoom(roomName);
        serverClient2.joinRoom(roomName);

        // Verify both clients are in the room
        assertTrue(serverClient1.getAllRooms().contains(roomName), "First client should be in the room");
        assertTrue(serverClient2.getAllRooms().contains(roomName), "Second client should be in the room");

        // Test room operations
        assertNotNull(getServer().getRoomOperations(roomName), "Room operations should not be null");
        getServer().getRoomOperations(roomName).sendEvent(testEvent, testData);

        // Wait for messages to be received
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> receivedData1.get() != null && receivedData2.get() != null);
        assertEquals(testData, receivedData1.get(), "Client 1 should receive the correct data");
        assertEquals(testData, receivedData2.get(), "Client 2 should receive the correct data");
        // Cleanup
        client1.disconnect();
        client1.close();
        client2.disconnect();
        client2.close();
    }
}
