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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.ConnectListener;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Test class for SocketIO transport upgrade functionality.
 * Tests the upgrade from HTTP long-polling to WebSocket as specified in Engine.IO protocol v4.
 */
@DisplayName("Transport Upgrade Tests - Engine.IO Protocol Transport Upgrade")
public class TransportUpgradeTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should upgrade from HTTP polling to WebSocket transport")
    public void testTransportUpgrade() throws Exception {
        // Test that client upgrades from polling to websocket
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        // Create client with default options (should upgrade to websocket)
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Verify connection is established
        assertNotNull(connectedClient.get(), "Client should be connected");
        assertTrue(client.connected(), "Client should be connected");

        // Wait for transport upgrade
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get().getTransport() == Transport.WEBSOCKET);
        // Assert transport is upgraded to WebSocket
        assertEquals(Transport.WEBSOCKET, connectedClient.get().getTransport(),
                "Expected transport to upgrade to WebSocket");
        client.disconnect();
    }

    @Test
    @DisplayName("Should work with HTTP polling transport only")
    public void testPollingOnlyTransport() throws Exception {
        // Test client that only uses polling transport (no upgrade)
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        // Create client with polling-only transport
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;
            options.transports = new String[]{"polling"}; // Only use polling

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Verify connection is established
        assertNotNull(connectedClient.get(), "Client should be connected");
        assertTrue(client.connected(), "Client should be connected");
        assertEquals(Transport.POLLING, connectedClient.get().getTransport(),
                "Expected transport to be Polling");

        client.disconnect();
    }

    @Test
    @DisplayName("Should work with WebSocket transport only")
    public void testWebSocketOnlyTransport() throws Exception {
        // Test client that only uses websocket transport
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        // Create client with websocket-only transport
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;
            options.transports = new String[]{"websocket"}; // Only use websocket

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Verify connection is established
        assertNotNull(connectedClient.get(), "Client should be connected");
        assertTrue(client.connected(), "Client should be connected");
        assertEquals(Transport.WEBSOCKET, connectedClient.get().getTransport(),
                "Expected transport to be WebSocket");
        client.disconnect();
    }

    @Test
    @DisplayName("Should handle transport upgrade for multiple concurrent clients")
    public void testMultipleClientsTransportUpgrade() throws Exception {
        // Test multiple clients with different transport preferences
        AtomicReference<SocketIOClient> connectedClient1 = new AtomicReference<>();
        AtomicReference<SocketIOClient> connectedClient2 = new AtomicReference<>();
        AtomicReference<SocketIOClient> connectedClient3 = new AtomicReference<>();
        final Object assignmentLock = new Object();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public synchronized void onConnect(SocketIOClient client) {
                // Simple round-robin assignment for testing, now thread-safe
                synchronized (assignmentLock) {
                    if (connectedClient1.get() == null) {
                        connectedClient1.set(client);
                    } else if (connectedClient2.get() == null) {
                        connectedClient2.set(client);
                    } else {
                        connectedClient3.set(client);
                    }
                }
            }
        });

        // Create clients with different transport preferences
        Socket client1; // Default (should upgrade)
        Socket client2; // Polling only
        Socket client3; // WebSocket only

        try {
            IO.Options options1 = new IO.Options();
            options1.timeout = 10000;
            options1.forceNew = true;
            client1 = IO.socket("http://localhost:" + getServerPort(), options1);

            IO.Options options2 = new IO.Options();
            options2.timeout = 10000;
            options2.forceNew = true;
            options2.transports = new String[]{"polling"};
            client2 = IO.socket("http://localhost:" + getServerPort(), options2);

            IO.Options options3 = new IO.Options();
            options3.timeout = 10000;
            options3.forceNew = true;
            options3.transports = new String[]{"websocket"};
            client3 = IO.socket("http://localhost:" + getServerPort(), options3);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket clients", e);
        }

        // Connect all clients
        client1.connect();
        client2.connect();
        client3.connect();

        // Wait for all connections
        await().atMost(10, SECONDS)
                .until(() -> connectedClient1.get() != null &&
                        connectedClient2.get() != null &&
                        connectedClient3.get() != null);

        // Verify all connections are established
        assertNotNull(connectedClient1.get(), "Client 1 should be connected");
        assertNotNull(connectedClient2.get(), "Client 2 should be connected");
        assertNotNull(connectedClient3.get(), "Client 3 should be connected");

        assertTrue(client1.connected(), "Client 1 should be connected");
        assertTrue(client2.connected(), "Client 2 should be connected");
        assertTrue(client3.connected(), "Client 3 should be connected");

        // Disconnect all clients
        client1.disconnect();
        client2.disconnect();
        client3.disconnect();
    }
}
