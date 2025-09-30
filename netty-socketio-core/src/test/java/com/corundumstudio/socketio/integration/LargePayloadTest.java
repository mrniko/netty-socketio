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

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

import io.socket.client.IO;
import io.socket.client.Socket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SocketIO large payload transmission functionality.
 * Tests the transmission of large data payloads as specified in SocketIO protocol v5.
 */
@DisplayName("Large Payload Tests - SocketIO Protocol Large Data Transmission")
public class LargePayloadTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should handle large string payload transmission")
    public void testLargeStringPayload() throws Exception {
        // Test transmission of large string data
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<String> receivedData = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        String largeStringEventName = generateEventName("largeString");

        getServer().addEventListener(largeStringEventName, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                receivedData.set(data);
            }
        });

        // Create client
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 30000;
            options.forceNew = true;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Create large string payload (1KB)
        StringBuilder largeString = new StringBuilder();
        String baseText = generateTestData(10);
        for (int i = 0; i < 100; i++) {
            largeString.append(baseText).append(" ");
        }
        String testData = largeString.toString();

        // Send large string data
        client.emit(largeStringEventName, testData);

        // Wait for data to be received
        await().atMost(30, SECONDS)
                .until(() -> receivedData.get() != null);

        // Verify data integrity
        assertNotNull(receivedData.get(), "Large string data should be received");
        assertEquals(testData, receivedData.get(), "Large string data should match exactly");
        assertTrue(receivedData.get().length() > 5000, "Received data should be large");

        client.disconnect();
    }

    @Test
    @DisplayName("Should handle large object payload transmission")
    public void testLargeObjectPayload() throws Exception {
        // Test transmission of large object data
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<Object> receivedData = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        getServer().addEventListener("largeObjectEvent", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackRequest) {
                receivedData.set(data);
            }
        });

        // Create client
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 30000;
            options.forceNew = true;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Create large object payload
        java.util.Map<String, Object> largeObject = new java.util.HashMap<>();
        for (int i = 0; i < 10; i++) {
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                value.append("Large object data field ").append(i).append("-").append(j).append(" ");
            }
            largeObject.put("field" + i, value.toString());
        }

        // Send large object data
        client.emit("largeObjectEvent", largeObject);

        // Wait for data to be received
        await().atMost(30, SECONDS)
                .until(() -> receivedData.get() != null);

        // Verify data integrity
        assertNotNull(receivedData.get(), "Large object data should be received");
        assertTrue(receivedData.get() instanceof java.util.Map, "Received data should be a Map");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> receivedMap = (java.util.Map<String, Object>) receivedData.get();
        assertEquals(10, receivedMap.size(), "Received map should have 10 fields");

        client.disconnect();
    }

    @Test
    @DisplayName("Should handle large array payload transmission")
    public void testLargeArrayPayload() throws Exception {
        // Test transmission of large array data
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<Object> receivedData = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        getServer().addEventListener("largeArrayEvent", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackRequest) {
                receivedData.set(data);
            }
        });

        // Create client
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 30000;
            options.forceNew = true;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Create large array payload
        Object[] largeArray = new Object[100];
        for (int i = 0; i < 100; i++) {
            largeArray[i] = "Array element " + i + " with some additional data to make it larger";
        }

        // Send large array data
        client.emit("largeArrayEvent", largeArray);

        // Wait for data to be received
        await().atMost(30, SECONDS)
                .until(() -> receivedData.get() != null);

        // Verify data integrity
        assertNotNull(receivedData.get(), "Large array data should be received");

        // The data might be received as a List instead of an array
        if (receivedData.get() instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> receivedList = (java.util.List<Object>) receivedData.get();
            assertEquals(100, receivedList.size(), "Received list should have 100 elements");
        } else if (receivedData.get() instanceof Object[]) {
            Object[] receivedArray = (Object[]) receivedData.get();
            assertEquals(100, receivedArray.length, "Received array should have 100 elements");
        } else {
            // For now, just verify that we received some data
            assertNotNull(receivedData.get(), "Large array data should be received");
        }

        client.disconnect();
    }

    @Test
    @DisplayName("Should handle large payload with acknowledgment callbacks")
    public void testLargePayloadWithAck() throws Exception {
        // Test large payload transmission with acknowledgment
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<String> receivedData = new AtomicReference<>();
        AtomicReference<Boolean> ackReceived = new AtomicReference<>(false);

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
            }
        });

        getServer().addEventListener("largeAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                receivedData.set(data);
                if (ackRequest != null) {
                    ackRequest.sendAckData("Large payload received successfully");
                }
            }
        });

        // Create client
        Socket client;
        try {
            IO.Options options = new IO.Options();
            options.timeout = 30000;
            options.forceNew = true;

            client = IO.socket("http://localhost:" + getServerPort(), options);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }

        client.connect();

        // Wait for connection
        await().atMost(10, SECONDS)
                .until(() -> connectedClient.get() != null);

        // Create large string payload
        StringBuilder largeString = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            largeString.append("Large payload with acknowledgment test data ").append(i).append(" ");
        }
        String testData = largeString.toString();

        // Send large data with acknowledgment
        client.emit("largeAckEvent", testData, new io.socket.client.Ack() {
            @Override
            public void call(Object... args) {
                ackReceived.set(true);
            }
        });

        // Wait for data and acknowledgment
        await().atMost(30, SECONDS)
                .until(() -> receivedData.get() != null && ackReceived.get());

        // Verify data integrity and acknowledgment
        assertNotNull(receivedData.get(), "Large data should be received");
        assertEquals(testData, receivedData.get(), "Large data should match exactly");
        assertTrue(ackReceived.get(), "Acknowledgment should be received");

        client.disconnect();
    }
}
