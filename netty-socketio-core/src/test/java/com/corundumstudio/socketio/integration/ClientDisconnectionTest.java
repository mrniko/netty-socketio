/**
 * Copyright (c) 2012-2025 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import com.corundumstudio.socketio.listener.DisconnectListener;

import io.socket.client.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO client disconnection functionality.
 */
@DisplayName("Client Disconnection Tests - SocketIO Protocol DISCONNECT")
public class ClientDisconnectionTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should handle client disconnection and trigger server disconnect listener")
    public void testClientDisconnection() throws Exception {
        // Test client disconnection
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch disconnectLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                disconnectLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");
        assertNotNull(connectedClient.get(), "Connected client should not be null");

        // Verify client is connected
        assertEquals(1, getServer().getAllClients().size(), "Server should have one connected client");

        // Disconnect client
        client.disconnect();
        client.close();

        // Wait for disconnection
        assertTrue(disconnectLatch.await(10, TimeUnit.SECONDS), "Client should disconnect within 10 seconds");

        // Verify client is removed from server
        assertEquals(0, getServer().getAllClients().size(), "Server should have no connected clients");
    }
}
