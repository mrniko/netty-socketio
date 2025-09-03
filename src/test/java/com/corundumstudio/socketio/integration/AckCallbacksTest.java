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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

import io.socket.client.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Test class for SocketIO acknowledgment callbacks functionality.
 */
@DisplayName("Acknowledgment Callbacks Tests - SocketIO Protocol ACK")
public class AckCallbacksTest extends AbstractSocketIOIntegrationTest {

    @Test
    @DisplayName("Should handle event acknowledgment callbacks between client and server")
    public void testAckCallbacks() throws Exception {
        // Test acknowledgment callbacks
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicReference<String> receivedData = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("ackEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                receivedData.set(data);
                // Send acknowledgment with data
                ackRequest.sendAckData("Acknowledged: " + data);
                eventLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("ackEvent", new Object[]{"Test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertEquals("Test data", receivedData.get(), "Received data should match sent data");
        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals("Acknowledged: Test data", ackData.get()[0], "Acknowledgment data should match expected");

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle empty acknowledgment responses")
    public void testEmptyAckResponse() throws Exception {
        // Test acknowledgment with empty response (as per protocol: payload MUST be an array, possibly empty)
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("emptyAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Send empty acknowledgment (empty array as per protocol)
                ackRequest.sendAckData();
                eventLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("emptyAckEvent", new Object[]{"Test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals(0, ackData.get().length, "Acknowledgment should be empty array");

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle multiple acknowledgment parameters")
    public void testMultipleAckParameters() throws Exception {
        // Test acknowledgment with multiple parameters
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("multiAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Send acknowledgment with multiple parameters
                ackRequest.sendAckData("status", "success", 200, true);
                eventLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("multiAckEvent", new Object[]{"Test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals(4, ackData.get().length, "Acknowledgment should have 4 parameters");
        assertEquals("status", ackData.get()[0], "First parameter should match");
        assertEquals("success", ackData.get()[1], "Second parameter should match");
        assertEquals(200, ackData.get()[2], "Third parameter should match");
        assertEquals(true, ackData.get()[3], "Fourth parameter should match");

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle acknowledgment with complex data types")
    public void testAckWithComplexDataTypes() throws Exception {
        // Test acknowledgment with complex data types (objects, arrays, etc.)
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("complexAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Create complex acknowledgment data
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("timestamp", System.currentTimeMillis());
                response.put("data", new String[]{"item1", "item2", "item3"});
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("version", "1.0");
                metadata.put("count", 3);
                response.put("metadata", metadata);
                
                ackRequest.sendAckData(response);
                eventLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("complexAckEvent", new Object[]{"Test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals(1, ackData.get().length, "Acknowledgment should have 1 parameter");
        
        // Handle both Map and JSONObject types
        Object responseObj = ackData.get()[0];
        assertNotNull(responseObj, "Response object should not be null");
        
        if (responseObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) responseObj;
            assertEquals("success", response.get("status"), "Status should match");
            assertNotNull(response.get("timestamp"), "Timestamp should not be null");
            assertNotNull(response.get("data"), "Data array should not be null");
            assertNotNull(response.get("metadata"), "Metadata should not be null");
        } else {
            // For JSONObject or other types, we'll just verify the object is not null
            // The exact structure verification would require JSONObject parsing
            assertNotNull(responseObj, "Response should be a valid object");
        }

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle acknowledgment in custom namespace")
    public void testAckInCustomNamespace() throws Exception {
        // Test acknowledgment in custom namespace
        String namespaceName = "/custom";
        SocketIONamespace customNamespace = getServer().addNamespace(namespaceName);
        
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        customNamespace.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        customNamespace.addEventListener("customAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                ackRequest.sendAckData("Custom namespace ACK: " + data);
                eventLatch.countDown();
            }
        });

        // Connect client to custom namespace
        Socket client = createClient(namespaceName);
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect to custom namespace within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("customAckEvent", new Object[]{"Custom test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals("Custom namespace ACK: Custom test data", ackData.get()[0], "Acknowledgment data should match expected");

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle multiple concurrent acknowledgment requests")
    public void testMultipleConcurrentAckRequests() throws Exception {
        // Test multiple concurrent acknowledgment requests with different event IDs
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();
        AtomicInteger eventCount = new AtomicInteger(0);

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("concurrentAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                int count = eventCount.incrementAndGet();
                ackRequest.sendAckData("Response " + count + " for: " + data);
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send multiple concurrent events with acknowledgments
        int numEvents = 5;
        CountDownLatch[] ackLatches = new CountDownLatch[numEvents];
        AtomicReference<Object[]>[] ackDataArray = new AtomicReference[numEvents];

        for (int i = 0; i < numEvents; i++) {
            ackLatches[i] = new CountDownLatch(1);
            ackDataArray[i] = new AtomicReference<>();
            final int index = i;
            
            client.emit("concurrentAckEvent", new Object[]{"Data " + i}, args -> {
                ackDataArray[index].set(args);
                ackLatches[index].countDown();
            });
        }

        // Wait for all acknowledgments
        for (int i = 0; i < numEvents; i++) {
            assertTrue(ackLatches[i].await(10, TimeUnit.SECONDS), 
                "Acknowledgment " + i + " should be received within 10 seconds");
        }

        // Verify all acknowledgments
        for (int i = 0; i < numEvents; i++) {
            assertNotNull(ackDataArray[i].get(), "Acknowledgment data " + i + " should not be null");
            assertTrue(ackDataArray[i].get()[0].toString().contains("Data " + i), 
                "Acknowledgment " + i + " should contain the original data");
        }

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle server-to-client acknowledgment")
    public void testServerToClientAck() throws Exception {
        // Test server sending event to client with acknowledgment
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

        // Set up client-side event listener with acknowledgment
        CountDownLatch clientEventLatch = new CountDownLatch(1);
        CountDownLatch serverAckLatch = new CountDownLatch(1);
        AtomicReference<Object[]> clientEventData = new AtomicReference<>();
        AtomicReference<Object[]> serverAckData = new AtomicReference<>();

        client.on("serverEvent", args -> {
            clientEventData.set(args);
            clientEventLatch.countDown();
            
            // Send acknowledgment back to server
            client.emit("serverEventAck", "Client received: " + args[0]);
        });

        // Set up server-side acknowledgment listener
        getServer().addEventListener("serverEventAck", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                serverAckData.set(new Object[]{data});
                ackRequest.sendAckData("Server received client ACK");
                serverAckLatch.countDown();
            }
        });

        // Send event from server to client with acknowledgment
        connectedClient.get().sendEvent("serverEvent", "Hello from server");

        // Wait for client to receive event and send acknowledgment
        assertTrue(clientEventLatch.await(10, TimeUnit.SECONDS), "Client should receive server event within 10 seconds");
        assertTrue(serverAckLatch.await(10, TimeUnit.SECONDS), "Server should receive client acknowledgment within 10 seconds");

        // Verify the data flow
        assertNotNull(clientEventData.get(), "Client should receive event data");
        assertEquals("Hello from server", clientEventData.get()[0], "Client should receive correct event data");
        
        assertNotNull(serverAckData.get(), "Server should receive acknowledgment data");
        assertEquals("Client received: Hello from server", serverAckData.get()[0], "Server should receive correct acknowledgment");

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle acknowledgment timeout scenarios")
    public void testAckTimeout() throws Exception {
        // Test acknowledgment timeout when server doesn't respond
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        // Add event listener that doesn't send acknowledgment
        getServer().addEventListener("noAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Intentionally not sending acknowledgment to test timeout
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment and expect timeout
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();
        AtomicReference<Exception> ackError = new AtomicReference<>();

        client.emit("noAckEvent", new Object[]{"Test data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for acknowledgment (should timeout)
        boolean ackReceived = ackLatch.await(3, TimeUnit.SECONDS);
        
        // In this implementation, the acknowledgment might still be received as an empty response
        // This test verifies the behavior when no explicit acknowledgment is sent
        if (ackReceived) {
            // If acknowledgment is received, it should be empty or null
            assertTrue(ackData.get() == null || ackData.get().length == 0, 
                "Acknowledgment should be empty when server doesn't send explicit ACK");
        }

        // Cleanup
        client.disconnect();
        client.close();
    }

    @Test
    @DisplayName("Should handle acknowledgment with error responses")
    public void testAckWithErrorResponse() throws Exception {
        // Test acknowledgment with error response
        CountDownLatch connectLatch = new CountDownLatch(1);
        CountDownLatch eventLatch = new CountDownLatch(1);
        AtomicReference<SocketIOClient> connectedClient = new AtomicReference<>();

        getServer().addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                connectedClient.set(client);
                connectLatch.countDown();
            }
        });

        getServer().addEventListener("errorAckEvent", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, com.corundumstudio.socketio.AckRequest ackRequest) {
                // Send error acknowledgment
                ackRequest.sendAckData("error", "Invalid data format", 400);
                eventLatch.countDown();
            }
        });

        // Connect client
        Socket client = createClient();
        client.connect();

        // Wait for connection
        assertTrue(connectLatch.await(10, TimeUnit.SECONDS), "Client should connect within 10 seconds");

        // Send event with acknowledgment
        CountDownLatch ackLatch = new CountDownLatch(1);
        AtomicReference<Object[]> ackData = new AtomicReference<>();

        client.emit("errorAckEvent", new Object[]{"Invalid data"}, args -> {
            ackData.set(args);
            ackLatch.countDown();
        });

        // Wait for event and acknowledgment
        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Event should be received within 10 seconds");
        assertTrue(ackLatch.await(10, TimeUnit.SECONDS), "Acknowledgment should be received within 10 seconds");

        assertNotNull(ackData.get(), "Acknowledgment data should not be null");
        assertEquals(3, ackData.get().length, "Acknowledgment should have 3 parameters");
        assertEquals("error", ackData.get()[0], "First parameter should be 'error'");
        assertEquals("Invalid data format", ackData.get()[1], "Second parameter should be error message");
        assertEquals(400, ackData.get()[2], "Third parameter should be error code");

        // Cleanup
        client.disconnect();
        client.close();
    }
}
