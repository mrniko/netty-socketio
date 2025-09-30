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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.PingListener;
import com.corundumstudio.socketio.listener.PongListener;

import io.socket.client.IO;
import io.socket.client.Socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO heartbeat mechanism and connection timeout functionality.
 * Tests PING/PONG heartbeat mechanism as specified in Engine.IO protocol v4.
 */
@DisplayName("Heartbeat Tests - Engine.IO Protocol PING/PONG & Connection Timeouts")
public class HeartbeatTest extends AbstractSocketIOIntegrationTest {
    @Override
    protected void configureServer(Configuration config) {
        super.configureServer(config);
        // 2s ping interval, 6s timeout
        config.setPingInterval(2000);
        config.setPingTimeout(6000);
    }

    @Test
    @DisplayName("Should maintain connection through heartbeat mechanism")
    public void testHeartbeatMechanism() throws Exception {
        AtomicReference<SocketIOClient> heartBeatClient = new AtomicReference<>();

        //currently socket.io client does not respond to server ping packet
        //we keep both ping and pong listeners for future proofing

        getServer().addPongListener(new PongListener() {
            @Override
            public void onPong(SocketIOClient client) {
                heartBeatClient.set(client);
            }
        });

        getServer().addPingListener(new PingListener() {
            @Override
            public void onPing(SocketIOClient client) {
                heartBeatClient.set(client);
            }
        });

        // Create client with custom options
        Socket client;
        IO.Options options = new IO.Options();
        client = IO.socket("http://localhost:" + getServerPort(), options);
        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> heartBeatClient.get() != null);

        // Wait additional time over timeout to ensure connection is kept alive by heartbeats
        TimeUnit.SECONDS.sleep(10);

        // Verify connection is still alive
        assertNotNull(heartBeatClient.get(), "Client should be connected");
        assertTrue(client.connected(), "Connection should still be active");

        client.disconnect();
    }
}
