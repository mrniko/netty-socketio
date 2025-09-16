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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;

import io.socket.client.Socket;
import io.socket.parser.Packet;
import io.socket.parser.Parser;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for SocketIO binary data transmission functionality.
 * Tests BINARY_EVENT and BINARY_ACK packet types as specified in SocketIO protocol v5.
 */
@DisplayName("Binary Data Tests - SocketIO Protocol BINARY_EVENT & BINARY_ACK")
public class BinaryDataTest extends AbstractSocketIOIntegrationTest {
    private static final Field SOCKET_IO_SEND_BUFFER;
    private static final Method EMIT_BUFFERED;

    static {
        try {
            SOCKET_IO_SEND_BUFFER = Socket.class.getDeclaredField("sendBuffer");
            SOCKET_IO_SEND_BUFFER.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to access sendBuffer field in Socket class", e);
        }
        try {
            EMIT_BUFFERED = Socket.class.getDeclaredMethod("emitBuffered");
            EMIT_BUFFERED.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to access emitBuffered method in Socket class", e);
        }
    }

    private String binaryEventName;

    @Test
    @DisplayName("Should transmit binary data from client to server using BINARY_EVENT")
    public void testBinaryEventTransmission() throws Exception {
        // Test sending binary data from client to server
        binaryEventName = generateEventName("binary");
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<Object> receivedData = new AtomicReference<>();

        getServer().addConnectListener(connectedClient::set);

        getServer().addEventListener(
                binaryEventName, Object.class,
                (client, data, ackRequest) -> {
                    receivedData.set(data);
                }
        );

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Send binary data through reflection to access private sendBuffer
        String testData = generateTestData();

        Queue<Packet<JSONArray>> sendBuffer = (Queue<Packet<JSONArray>>) SOCKET_IO_SEND_BUFFER.get(client);
        sendBuffer.add(new Packet<>(Parser.BINARY_EVENT, new JSONArray().put(binaryEventName).put(testData)));
        EMIT_BUFFERED.invoke(client);

        // Wait for data to be received
        await().atMost(10, SECONDS)
                .until(() -> receivedData.get() != null);

        // Verify data integrity
        assertNotNull(receivedData.get());
        assertEquals(testData, receivedData.get());
    }
}
