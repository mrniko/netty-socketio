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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;

import io.socket.client.Socket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO room management functionality.
 */
@DisplayName("Room Management Tests - SocketIO Protocol ROOMS")
public class RoomManagementTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should allow client to join and leave rooms successfully")
    public void testRoomManagement() throws Exception {
        // Test room joining and leaving
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");
        SocketIOClient serverClient = connectedClient.get();

        // Join room
        String roomName = generateRoomName();
        serverClient.joinRoom(roomName);

        // Verify client is in room
        assertTrue(serverClient.getAllRooms().contains(roomName), "Client should be in the room");

        // Leave room
        serverClient.leaveRoom(roomName);

        // Verify client left room
        assertFalse(serverClient.getAllRooms().contains(roomName), "Client should not be in the room");

        // Cleanup
        client.disconnect();
        client.close();
    }
}
