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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.store.CustomizedRedisContainer;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.github.javafaker.Faker;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Abstract base class for SocketIO integration tests.
 * Provides common setup, teardown, and utility methods.
 *
 * Features:
 * - Automatic Redis container management
 * - Dynamic port allocation for concurrent testing
 * - Common SocketIO server configuration
 * - Utility methods for client creation and management
 */
public abstract class AbstractSocketIOIntegrationTest {

    protected final Faker faker = new Faker();

    private GenericContainer<?> redisContainer;
    private SocketIOServer server;
    private RedissonClient redissonClient;
    private int serverPort;

    private static final String SERVER_HOST = "localhost";
    private static final int BASE_PORT = 9000;
    private static final int PORT_RANGE = 2000; // Increased range for better distribution
    private static final AtomicInteger PORT_COUNTER = new AtomicInteger(0);
    private static final int MAX_PORT_RETRIES = 5;

    /**
     * Get the current server port for this test instance
     */
    protected int getServerPort() {
        return serverPort;
    }

    /**
     * Get the server host
     */
    protected String getServerHost() {
        return SERVER_HOST;
    }

    /**
     * Get the SocketIO server instance
     */
    protected SocketIOServer getServer() {
        return server;
    }

    /**
     * Get the Redisson client instance
     */
    protected RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * Get the Redis container
     */
    protected GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }

    /**
     * Create a Socket.IO client connected to the test server
     */
    protected Socket createClient() {
        try {
            return IO.socket("http://" + SERVER_HOST + ":" + serverPort);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client", e);
        }
    }

    /**
     * Create a Socket.IO client connected to a specific namespace
     */
    protected Socket createClient(String namespace) {
        try {
            return IO.socket("http://" + SERVER_HOST + ":" + serverPort + namespace);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create socket client for namespace: " + namespace, e);
        }
    }

    /**
     * Allocate a unique port for this test instance.
     * Uses atomic counter to ensure thread-safe port allocation.
     */
    private synchronized int allocatePort() {
        int portIndex = PORT_COUNTER.getAndIncrement();
        int port = BASE_PORT + (portIndex % PORT_RANGE);

        // If we've used all ports in the range, reset counter
        if (portIndex >= PORT_RANGE) {
            PORT_COUNTER.set(0);
        }
        return port;
    }

    /**
     * Find an available port with retry mechanism
     */
    private int findAvailablePort() throws Exception {
        for (int attempt = 0; attempt < MAX_PORT_RETRIES; attempt++) {
            int port = allocatePort();
            if (isPortAvailable(port)) {
                return port;
            }
            // Wait a bit before retrying
            Thread.sleep(100);
        }
        throw new RuntimeException("Could not find available port after " + MAX_PORT_RETRIES + " attempts");
    }

    /**
     * Check if a port is available
     */
    private boolean isPortAvailable(int port) {
        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Setup method called before each test.
     * Initializes Redis container, Redisson client, and SocketIO server.
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Start Redis container
        redisContainer = new CustomizedRedisContainer();
        redisContainer.start();

        // Configure Redisson client
        CustomizedRedisContainer customizedRedisContainer = (CustomizedRedisContainer) redisContainer;
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + customizedRedisContainer.getHost() + ":" + customizedRedisContainer.getRedisPort());

        redissonClient = Redisson.create(config);

        // Create SocketIO server configuration
        Configuration serverConfig = new Configuration();
        serverConfig.setHostname(SERVER_HOST);

        // Find an available port for this test
        serverPort = findAvailablePort();
        serverConfig.setPort(serverPort);
        serverConfig.setStoreFactory(new RedissonStoreFactory(redissonClient));

        // Allow subclasses to customize configuration
        configureServer(serverConfig);

        // Create and start server
        server = new SocketIOServer(serverConfig);
        server.start();

        // Verify server started successfully
        if (serverPort <= 0) {
            throw new RuntimeException("Failed to start server on port: " + serverPort);
        }

        // Allow subclasses to do additional setup
        additionalSetup();
    }

    /**
     * Teardown method called after each test.
     * Cleans up all resources to ensure test isolation.
     */
    @AfterEach
    public void tearDown() throws Exception {
        // Allow subclasses to do additional teardown
        additionalTeardown();

        // Stop SocketIO server
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Log but don't fail the test
                System.err.println("Error stopping SocketIO server: " + e.getMessage());
            }
        }

        // Shutdown Redisson client
        if (redissonClient != null) {
            try {
                redissonClient.shutdown();
            } catch (Exception e) {
                // Log but don't fail the test
                System.err.println("Error shutting down Redisson client: " + e.getMessage());
            }
        }

        // Stop Redis container
        if (redisContainer != null && redisContainer.isRunning()) {
            try {
                redisContainer.stop();
            } catch (Exception e) {
                // Log but don't fail the test
                System.err.println("Error stopping Redis container: " + e.getMessage());
            }
        }
    }

    /**
     * Hook method for subclasses to add custom server configuration.
     * Called after basic configuration but before server start.
     */
    protected void configureServer(Configuration config) {
        // Default implementation does nothing
        // Subclasses can override to add custom configuration
    }

    /**
     * Hook method for subclasses to add custom setup logic.
     * Called after server start.
     */
    protected void additionalSetup() throws Exception {
        // Default implementation does nothing
        // Subclasses can override to add custom setup
    }

    /**
     * Hook method for subclasses to add custom teardown logic.
     * Called before resource cleanup.
     */
    protected void additionalTeardown() throws Exception {
        // Default implementation does nothing
        // Subclasses can override to add custom teardown
    }

    /**
     * Generate a random event name using faker
     */
    protected String generateEventName() {
        return faker.lorem().word() + "Event";
    }

    /**
     * Generate a random event name with a specific prefix
     */
    protected String generateEventName(String prefix) {
        return prefix + faker.lorem().word() + "Event";
    }

    /**
     * Generate a random event name with a specific suffix
     */
    protected String generateEventNameWithSuffix(String suffix) {
        return faker.lorem().word() + suffix;
    }

    /**
     * Generate a random test data string
     */
    protected String generateTestData() {
        return faker.lorem().sentence();
    }

    /**
     * Generate a random test data string with specific length
     */
    protected String generateTestData(int wordCount) {
        return faker.lorem().sentence(wordCount);
    }

    /**
     * Generate a random room name
     */
    protected String generateRoomName() {
        return faker.lorem().word() + "Room";
    }

    /**
     * Generate a random room name with a specific prefix
     */
    protected String generateRoomName(String prefix) {
        return prefix + faker.lorem().word() + "Room";
    }

    /**
     * Generate a random namespace name
     */
    protected String generateNamespaceName() {
        return "/" + faker.lorem().word();
    }

    /**
     * Generate a random namespace name with a specific prefix
     */
    protected String generateNamespaceName(String prefix) {
        return "/" + prefix + faker.lorem().word();
    }

    /**
     * Generate a random acknowledgment message
     */
    protected String generateAckMessage() {
        return "Acknowledged: " + faker.lorem().sentence();
    }

    /**
     * Generate a random acknowledgment message with specific data
     */
    protected String generateAckMessage(String data) {
        return "Acknowledged: " + data;
    }

    /**
     * Generate a random error message
     */
    protected String generateErrorMessage() {
        return faker.lorem().sentence() + " error";
    }

    /**
     * Generate a random status message
     */
    protected String generateStatusMessage() {
        return faker.lorem().word() + " status: " + faker.lorem().sentence();
    }
}
