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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;

import io.socket.client.Socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for basic SocketIO client connection functionality.
 */
@DisplayName("Basic Connection Tests - SocketIO Protocol CONNECT/DISCONNECT")
public class BasicConnectionTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should establish basic client connection and trigger server connect listener")
    public void testBasicClientConnection() throws Exception {
        // Test basic client connection
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(connectedClient::set);

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection using Awaitility
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        assertNotNull(connectedClient.get(), "Connected client should not be null");

        // Verify client is in server's client list
        await().atMost(5, SECONDS)
                .until(() -> getServer().getAllClients().contains(connectedClient.get()));

        // Disconnect client
        client.disconnect();
        client.close();
    }
}
