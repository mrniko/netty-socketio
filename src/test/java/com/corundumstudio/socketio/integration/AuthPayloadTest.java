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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

import io.socket.client.IO;
import io.socket.client.Socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO authentication payload functionality.
 * Tests authentication payload handling during connection as specified in SocketIO protocol v5.
 */
@DisplayName("Authentication Payload Tests - SocketIO Protocol CONNECT with Auth")
public class AuthPayloadTest extends AbstractSocketIOIntegrationTest {
    private final String authUserIdKey = "userId";
    private final String authUserId = faker.internet().uuid();
    private final String authUserPasswordKey = "password";
    private final String authUserPassword = faker.internet().password();

    @Override
    protected void additionalSetup() throws Exception {
        super.additionalSetup();
        getServer().getAllNamespaces().forEach(ns -> {
            ns.addAuthTokenListener((authToken, client) -> {
                if (authToken instanceof Map) {
                    Map<String, String> authMap = (Map<String, String>) authToken;
                    String userId = authMap.get(authUserIdKey);
                    String password = authMap.get(authUserPasswordKey);
                    if (authUserId.equals(userId) && authUserPassword.equals(password)) {
                        return AuthTokenResult.AUTH_TOKEN_RESULT_SUCCESS;
                    }
                }
                return new AuthTokenResult(false, "Invalid authentication payload");
            });
        });
    }

    @Test
    @DisplayName("Should connect successfully with authentication payload")
    public void testConnectionWithAuthPayload() throws Exception {
        // Test connection with authentication payload
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicBoolean receivedEvent = new AtomicBoolean(false);

        getServer().addConnectListener(connectedClient::set);

        String testEventName = generateEventName();

        getServer().addEventListener(testEventName, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {
                receivedEvent.set(true);
            }
        });

        // Create client with auth payload
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.auth = new HashMap<>();
            options.auth.put(authUserIdKey, authUserId);
            options.auth.put(authUserPasswordKey, authUserPassword);

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();
        // Emit a test event to ensure connection is fully established
        client.emit(testEventName, faker.address().fullAddress());

        // Wait for connection
        await().atMost(10, SECONDS).until(() -> connectedClient.get() != null);
        // Wait for event reception
        await().atMost(10, SECONDS).until(receivedEvent::get);

        // Verify connection succeeded
        assertNotNull(connectedClient.get(), "Client should be connected");
        // Verify event was received
        assertTrue(receivedEvent.get(), "Should receive event with valid auth");
        // Verify client connection state
        assertTrue(client.connected());

        client.disconnect();
    }

    @Test
    @DisplayName("Should handle connection with empty authentication payload")
    public void testConnectionWithEmptyAuthPayload() throws Exception {
        // Test connection with empty authentication payload
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicBoolean receivedEvent = new AtomicBoolean(false);

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        getServer().addEventListener(
                "testEvent", String.class,
                (client, data, ackSender) -> receivedEvent.set(true)
        );

        // Create client with empty auth payload
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.auth = new HashMap<>();
            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Emit a test event to ensure connection is fully established
        client.emit("testEvent", "testData");

        // Wait for connection
        await().atMost(10, SECONDS).until(() -> connectedClient.get() != null);
        // force wait for event reception
        SECONDS.sleep(5);

        // Verify connection succeeded even with empty auth
        assertNotNull(connectedClient.get(), "Client should be connected");
        // Verify event was not received due to failed auth
        assertFalse(receivedEvent.get(), "Should not receive event with empty auth");
        // Verify client connection state
        assertFalse(client.connected());

        client.disconnect();
    }
}
