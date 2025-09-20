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

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import io.socket.client.IO;
import io.socket.client.Socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO session recovery functionality.
 * Tests session recovery and reconnection scenarios as specified in SocketIO protocol v5.
 */
@DisplayName("Session Recovery Tests - SocketIO Protocol Session Recovery & Reconnection")
public class SessionRecoveryTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should recover session after client disconnection")
    public void testSessionRecoveryAfterDisconnection() throws Exception {
        // Test session recovery after client disconnection
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<Boolean> disconnected = new AtomicReference<>(false);
        AtomicReference<Boolean> reconnected = new AtomicReference<>(false);

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                if (connectedClient.get() == null) {
                    connectedClient.set(client);
                } else {
                    reconnected.set(true);
                }
            }
        });

        getServer().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                disconnected.set(true);
            }
        });

        // Create client with reconnection enabled
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 3;
            options.reconnectionDelay = 1000;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        // Connect client
        client.connect();

        // Wait for initial connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        assertNotNull(connectedClient.get(), "Client should be connected initially");

        // Disconnect client
        client.disconnect();

        // Wait for disconnection
        await().atMost(5, SECONDS)
                .until(() -> disconnected.get());

        assertTrue(disconnected.get(), "Client should be disconnected");

        // Reconnect client
        client.connect();

        // Wait for reconnection
        await().atMost(10, SECONDS)
                .until(() -> reconnected.get());

        assertTrue(reconnected.get(), "Client should be reconnected");

        client.disconnect();
    }

    @Test
    @DisplayName("Should handle session recovery for multiple clients")
    public void testSessionRecoveryWithMultipleClients() throws Exception {
        // Test session recovery with multiple clients
        AtomicReference<SocketIOClient> connectedClient1 = new AtomicReference<>();
        AtomicReference<SocketIOClient> connectedClient2 = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            //must be synchronized, because multiple clients can connect simultaneously
            public synchronized void onConnect(SocketIOClient client) {
                if (connectedClient1.get() == null) {
                    connectedClient1.set(client);
                } else if (connectedClient2.get() == null) {
                    connectedClient2.set(client);
                }
            }
        });

        // Create two clients with reconnection enabled
        Socket client1;
        Socket client2;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 3;
            options.reconnectionDelay = 1000;

            client1 = IO.socket("http://localhost:" + getServerPort(), options);
            client2 = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket clients", e);
        }

        // Connect both clients
        client1.connect();
        client2.connect();

        CountDownLatch initialConnected = new CountDownLatch(2);
        CountDownLatch bothDisconnected = new CountDownLatch(2);
        CountDownLatch bothReconnected = new CountDownLatch(2);
        AtomicBoolean reconnectPhase = new AtomicBoolean(false);

        // Replace existing connect listener to drive latches
        getServer().addConnectListener(new ConnectListener() {
            @Override
            public synchronized void onConnect(SocketIOClient client) {
                if (!reconnectPhase.get()) {
                    if (connectedClient1.get() == null) {
                        connectedClient1.set(client);
                        initialConnected.countDown();
                    } else if (connectedClient2.get() == null) {
                        connectedClient2.set(client);
                        initialConnected.countDown();
                    }
                } else {
                    bothReconnected.countDown();
                }
            }
        });
        getServer().addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                bothDisconnected.countDown();
            }
        });

        // Wait for both connections
        await().atMost(10, SECONDS).until(() -> initialConnected.getCount() == 0);
        assertNotNull(connectedClient1.get(), "Client 1 should be connected initially");
        assertNotNull(connectedClient2.get(), "Client 2 should be connected initially");

        // Disconnect and reconnect both clients
        client1.disconnect();
        client2.disconnect();
        await().atMost(5, SECONDS).until(() -> bothDisconnected.getCount() == 0);

        reconnectPhase.set(true);
        client1.connect();
        client2.connect();

        // Wait for both reconnections
        await().atMost(10, SECONDS).until(() -> bothReconnected.getCount() == 0);
        client1.disconnect();
        client2.disconnect();
    }

    @Test
    @DisplayName("Should recover session in custom namespace")
    public void testSessionRecoveryWithCustomNamespace() throws Exception {
        // Test session recovery with custom namespace
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<Boolean> reconnected = new AtomicReference<>(false);

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                if (connectedClient.get() == null) {
                    connectedClient.set(client);
                } else {
                    reconnected.set(true);
                }
            }
        });

        // Create client with reconnection enabled for custom namespace
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 10000;
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = 3;
            options.reconnectionDelay = 1000;

            client = IO.socket("http://localhost:" + getServerPort() + "/custom", options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        // Connect client
        client.connect();

        // Wait for initial connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        assertNotNull(connectedClient.get(), "Client should be connected initially");

        // Disconnect and reconnect
        client.disconnect();
        client.connect();

        // Wait for reconnection
        await().atMost(10, SECONDS)
                .until(() -> reconnected.get());

        assertTrue(reconnected.get(), "Client should be reconnected");

        client.disconnect();
    }
}
