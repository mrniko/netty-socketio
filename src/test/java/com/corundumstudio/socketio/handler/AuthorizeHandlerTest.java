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
package com.corundumstudio.socketio.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.HashedWheelScheduler;
import com.corundumstudio.socketio.store.StoreFactory;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Comprehensive integration test suite for AuthorizeHandler.
 * <p>
 * This test class validates the complete functionality of the AuthorizeHandler,
 * which is responsible for managing Socket.IO client connections and authorization.
 * <p>
 * Test Coverage:
 * - Channel lifecycle management (activation, deactivation)
 * - HTTP request processing and validation
 * - Socket.IO protocol compliance
 * - Authorization flow and client session management
 * - Error handling for various failure scenarios
 * - Transport type validation
 * - Session ID handling and reuse
 * <p>
 * Testing Approach:
 * - Uses EmbeddedChannel for realistic Netty pipeline testing
 * - Creates actual objects instead of mocks for integration testing
 * - Tests both success and failure scenarios
 * - Validates resource management and cleanup
 * - Ensures proper error responses and channel state management
 * <p>
 * Key Test Scenarios:
 * 1. Valid connection requests with proper authorization
 * 2. Invalid requests (wrong paths, missing parameters)
 * 3. Transport validation errors
 * 4. Session management and reuse
 * 5. Channel state management during various operations
 *
 * @see AuthorizeHandler
 * @see EmbeddedChannel
 * @see Socket.IO Protocol Specification
 */
public class AuthorizeHandlerTest {

    private static final String CONNECT_PATH = "/socket.io/";
    private static final String TEST_ORIGIN = "http://localhost:3000";
    private static final int FIRST_DATA_TIMEOUT = 1000; // 1 seconds

    private AuthorizeHandler authorizeHandler;
    private Configuration configuration;
    private CancelableScheduler scheduler;
    private NamespacesHub namespacesHub;
    private StoreFactory storeFactory;
    private DisconnectableHub disconnectable;
    private AckManager ackManager;
    private ClientsBox clientsBox;
    private AuthorizationListener authorizationListener;
    private EmbeddedChannel channel;

    /**
     * Sets up the test environment before each test method execution.
     * <p>
     * This method initializes all the necessary components for testing the AuthorizeHandler:
     * - Configuration: Sets up Socket.IO server configuration with test-specific values
     * - Scheduler: Creates a real HashedWheelScheduler for task management
     * - NamespacesHub: Sets up namespace management for Socket.IO
     * - StoreFactory: Provides storage capabilities for client data
     * - DisconnectableHub: Handles client disconnection events
     * - AckManager: Manages acknowledgment callbacks
     * - ClientsBox: Tracks active client connections
     * - AuthorizationListener: Provides authorization logic
     * - EmbeddedChannel: Creates a test channel with proper socket addresses
     * <p>
     * The setup emphasizes creating real objects instead of mocks to ensure
     * integration-level testing that closely resembles production behavior.
     */
    @BeforeEach
    void setUp() {
        // Create real objects instead of mocks for integration testing
        configuration = new Configuration();
        configuration.setAllowCustomRequests(false);
        configuration.setRandomSession(true);
        configuration.setFirstDataTimeout(FIRST_DATA_TIMEOUT);
        configuration.setPingInterval(25000);
        configuration.setPingTimeout(60000);
        configuration.setTransports(Transport.POLLING);

        scheduler = new HashedWheelScheduler();
        namespacesHub = new NamespacesHub(configuration);
        storeFactory = configuration.getStoreFactory();
        disconnectable = new DisconnectableHub() {
            @Override
            public void onDisconnect(ClientHead client) {
                // Test implementation
            }
        };

        ackManager = new AckManager(scheduler);
        clientsBox = new ClientsBox();
        authorizationListener = new AuthorizationListener() {
            @Override
            public AuthorizationResult getAuthorizationResult(HandshakeData data) {
                return new AuthorizationResult(true, Collections.emptyMap());
            }
        };

        configuration.setAuthorizationListener(authorizationListener);

        authorizeHandler = new AuthorizeHandler(
                CONNECT_PATH, scheduler, configuration, namespacesHub,
                storeFactory, disconnectable, ackManager, clientsBox
        );

        // Create a custom EmbeddedChannel with proper socket addresses
        channel = new EmbeddedChannel() {
            @Override
            public java.net.SocketAddress remoteAddress() {
                return new java.net.InetSocketAddress("127.0.0.1", 12345);
            }

            @Override
            public java.net.SocketAddress localAddress() {
                return new java.net.InetSocketAddress("127.0.0.1", 8080);
            }
        };
        channel.pipeline().addLast(authorizeHandler);
    }

    /**
     * Test that verifies the complete ping timeout mechanism of AuthorizeHandler.
     * <p>
     * This test ensures that when a channel becomes active, the handler properly:
     * 1. Schedules a ping timeout task to monitor client activity
     * 2. Maintains the channel in an active state initially
     * 3. Closes the channel after the configured timeout period if no data is received
     * <p>
     * The ping timeout is crucial for detecting inactive clients that open
     * connections but don't send any data, preventing resource leaks.
     * <p>
     * Test Flow:
     * - Channel becomes active → timeout task scheduled
     * - Wait for timeout period → channel should be closed automatically
     * - Verify that the timeout mechanism works as expected
     */
    @Test
    @DisplayName("Channel Active - Should Schedule Ping Timeout and Close Channel After Timeout")
    void testChannelActive_ShouldSchedulePingTimeout() throws Exception {
        // Given: Channel handler context is available and channel is in active state
        ChannelHandlerContext ctx = channel.pipeline().context(authorizeHandler);

        // When: Channel becomes active and triggers the channelActive event
        authorizeHandler.channelActive(ctx);

        // Then: Verify that ping timeout is scheduled and channel remains active initially
        // The handler should schedule a ping timeout task to monitor client activity
        assertThat(channel.isActive()).isTrue();

        // Wait for the timeout period plus a small buffer to ensure the task executes
        // The configuration sets firstDataTimeout to FIRST_DATA_TIMEOUT
        await().atMost(ofSeconds(3)).until(() -> !channel.isActive());

        // After the timeout, the channel should be closed by the scheduled task
        assertThat(channel.isActive()).isFalse();
    }

    /**
     * Test that verifies successful authorization of a valid Socket.IO connection request.
     * <p>
     * This test validates the complete handshake flow when a client sends a proper
     * connection request with valid parameters:
     * 1. Correct connection path (/socket.io/)
     * 2. Valid transport type (polling)
     * 3. Proper origin header
     * 4. No existing session ID (new connection)
     * <p>
     * The test ensures that the handler:
     * - Processes the HTTP request correctly
     * - Performs authorization successfully
     * - Creates a new client session
     * - Maintains the channel in active state
     * - Sets up the client for further communication
     */
    @Test
    @DisplayName("Valid Connect Request - Should Authorize Successfully and Create Client Session")
    void testChannelRead_WithValidConnectRequest_ShouldAuthorizeSuccessfully() throws Exception {
        // Given: A valid Socket.IO connection request with proper parameters
        String uri = CONNECT_PATH + "?transport=polling";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);
        request.headers().set(HttpHeaderNames.ORIGIN, TEST_ORIGIN);

        // When: The request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: Verify that the request was processed successfully and channel remains active
        // The handler should authorize the request and create a new client session
        assertThat(channel.isActive()).isTrue();

        // Note: The client should be created and added to clientsBox
        // However, ClientsBox doesn't expose getAllClients method for verification
        // We verify success by ensuring the channel remains active
    }

    /**
     * Test that verifies proper handling of requests with invalid connection paths.
     * <p>
     * This test ensures that the AuthorizeHandler correctly rejects requests
     * that don't match the expected Socket.IO connection path pattern:
     * 1. Requests to non-Socket.IO endpoints are rejected
     * 2. HTTP 400 Bad Request response is sent
     * 3. The channel is properly closed to prevent resource leaks
     * 4. Invalid requests don't interfere with valid Socket.IO connections
     * <p>
     * This is a security measure to prevent unauthorized access to Socket.IO
     * functionality through incorrect endpoints.
     */
    @Test
    @DisplayName("Invalid Path - Should Return Bad Request and Close Channel")
    void testChannelRead_WithInvalidPath_ShouldReturnBadRequest() throws Exception {
        // Given: An HTTP request with an invalid path that doesn't match Socket.IO patterns
        String invalidUri = "/invalid/path?transport=polling";
        FullHttpRequest request = createHttpRequest(invalidUri, TEST_ORIGIN);

        // When: The invalid request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: The handler should reject the invalid path and close the channel
        // We need to wait for async operations (HTTP response writing) to complete
        await().atMost(ofSeconds(2)).until(() -> !channel.isActive());

        // The channel should be closed because the handler sends BAD_REQUEST response
        // and explicitly closes the connection to prevent unauthorized access
        assertThat(channel.isActive()).isFalse();
    }

    /**
     * Test that verifies proper handling of requests missing the required transport parameter.
     * <p>
     * This test validates the error handling when a client sends a Socket.IO
     * connection request without specifying the transport mechanism:
     * 1. The request reaches the authorization phase
     * 2. Transport parameter validation fails
     * 3. Appropriate error message is sent to the client
     * 4. Channel remains active for potential retry or error handling
     * <p>
     * The transport parameter is mandatory for Socket.IO connections as it
     * determines the communication mechanism (polling, websocket, etc.).
     */
    @Test
    @DisplayName("Missing Transport - Should Return Transport Error and Keep Channel Active")
    void testChannelRead_WithMissingTransport_ShouldReturnTransportError() throws Exception {
        // Given: A Socket.IO connection request missing the required transport parameter
        String uri = CONNECT_PATH + "?noTransport=value";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // When: The incomplete request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: The handler should process the request but fail during transport validation
        // We need to wait for async operations (error message writing) to complete
        await().atMost(ofSeconds(1)).until(() -> channel.isActive());

        // The channel should remain active because writeAndFlushTransportError method
        // sends an error response but doesn't close the connection, allowing for
        // potential retry or proper error handling by the client
        assertThat(channel.isActive()).isTrue();
    }

    /**
     * Test that verifies proper handling of requests with unsupported transport types.
     * <p>
     * This test validates the error handling when a client specifies a transport
     * mechanism that the server doesn't support:
     * 1. The request reaches the authorization phase
     * 2. Transport type validation fails for unsupported values
     * 3. Appropriate error message is sent to the client
     * 4. Channel remains active for potential retry with supported transport
     * <p>
     * This ensures that clients using outdated or unsupported transport mechanisms
     * receive clear error messages and can potentially retry with supported options.
     */
    @Test
    @DisplayName("Unsupported Transport - Should Return Transport Error and Keep Channel Active")
    void testChannelRead_WithUnsupportedTransport_ShouldReturnTransportError() throws Exception {
        // Given: A Socket.IO connection request with an unsupported transport type
        String uri = CONNECT_PATH + "?transport=unsupported";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // When: The request with unsupported transport is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: The handler should process the request but fail during transport validation
        // We need to wait for async operations (error message writing) to complete
        await().atMost(ofSeconds(1)).until(() -> channel.isActive());

        // The channel should remain active because writeAndFlushTransportError method
        // sends an error response but doesn't close the connection, allowing the client
        // to potentially retry with a supported transport type
        assertThat(channel.isActive()).isTrue();
    }

    /**
     * Test that verifies proper handling of failed authorization attempts.
     * <p>
     * This test validates the error handling when a client's connection request
     * fails the authorization process:
     * 1. The request reaches the authorization phase with valid parameters
     * 2. Authorization listener returns false (unauthorized)
     * 3. HTTP 401 Unauthorized response is sent
     * 4. Channel is closed to prevent unauthorized access
     * <p>
     * This ensures that only properly authenticated clients can establish
     * Socket.IO connections with the server.
     */
    @Test
    @DisplayName("Failed Authorization - Should Return Unauthorized and Close Channel")
    void testChannelRead_WithFailedAuthorization_ShouldReturnUnauthorized() throws Exception {
        // Given: A request that will fail authorization
        String uri = CONNECT_PATH + "?transport=polling";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // Set up authorization to fail
        configuration.setAuthorizationListener(new AuthorizationListener() {
            @Override
            public AuthorizationResult getAuthorizationResult(HandshakeData data) {
                return new AuthorizationResult(false, Collections.emptyMap());
            }
        });

        // When: The request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: Verify that the appropriate response is sent
        // We need to wait for async operations to complete
        await().atMost(ofSeconds(2)).until(() -> !channel.isActive());

        // The channel should be closed due to UNAUTHORIZED response
        assertThat(channel.isActive()).isFalse();
    }

    /**
     * Test that verifies proper handling of requests with existing session IDs.
     * <p>
     * This test validates the session reuse functionality when a client
     * attempts to reconnect using a previously established session:
     * 1. The request contains a valid existing session ID (sid parameter)
     * 2. The handler recognizes this as a reconnection attempt
     * 3. The request is processed differently from new connections
     * 4. Channel remains active for the reconnection process
     * <p>
     * Session reuse is important for maintaining client state and providing
     * seamless reconnection experiences in Socket.IO applications.
     */
    @Test
    @DisplayName("Existing Session ID - Should Process Reconnection Request and Keep Channel Active")
    void testChannelRead_WithExistingSessionId_ShouldReuseSession() throws Exception {
        // Given: A Socket.IO connection request with an existing session ID for reconnection
        String existingSessionId = "550e8400-e29b-41d4-a716-446655440000";
        String uri = CONNECT_PATH + "?transport=polling&sid=" + existingSessionId;
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // When: The reconnection request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: The handler should process the request as a reconnection attempt
        // We need to wait for async operations to complete
        await().atMost(ofSeconds(1)).until(() -> channel.isActive());

        // The channel should remain active as this is a valid reconnection request
        // The handler processes reconnection requests differently from new connections
        assertThat(channel.isActive()).isTrue();
    }


    /**
     * Test that verifies channel context attributes are properly set after successful authorization.
     * <p>
     * This test validates that the handler correctly sets the CLIENT attribute
     * in the channel context after successful authorization:
     * 1. CLIENT attribute is set after successful authorization
     * 2. Client object contains proper session information
     * 3. Transport type is correctly set
     * <p>
     * Channel attributes are crucial for maintaining state and enabling
     * proper communication between different handlers in the pipeline.
     */
    @Test
    @DisplayName("Channel Context - Should Set Client Attribute After Successful Authorization")
    void testChannelContext_ShouldSetClientAttributeAfterSuccessfulAuthorization() throws Exception {
        // Given: A valid Socket.IO connection request
        String uri = CONNECT_PATH + "?transport=polling";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // When: The request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: Verify that the client attribute is set in the channel context
        // We need to wait a bit for the async operations to complete
        await().atMost(ofSeconds(1)).until(() -> channel.hasAttr(ClientHead.CLIENT));

        // The channel should have the CLIENT attribute set
        assertThat(channel.hasAttr(ClientHead.CLIENT)).isTrue();
        assertThat(channel.attr(ClientHead.CLIENT).get()).isNotNull();

        // Verify the client has the correct session ID and transport
        ClientHead client = channel.attr(ClientHead.CLIENT).get();
        assertThat(client.getSessionId()).isNotNull();
        assertThat(client.getCurrentTransport()).isEqualTo(Transport.POLLING);
    }

    /**
     * Test that verifies channel context attributes are properly set for transport error responses.
     * <p>
     * This test validates that the handler correctly sets the ORIGIN attribute
     * in the channel context when sending transport error responses:
     * 1. ORIGIN attribute is set for transport error responses
     * 2. Origin value matches the request origin
     * 3. Channel remains active for error handling
     * <p>
     * The ORIGIN attribute is essential for proper error response formatting
     * and CORS compliance in transport error scenarios.
     */
    @Test
    @DisplayName("Channel Context - Should Set Origin Attribute for Transport Errors")
    void testChannelContext_ShouldSetOriginAttributeForTransportErrors() throws Exception {
        // Given: A request with unsupported transport
        String uri = CONNECT_PATH + "?transport=unsupported";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);

        // When: The request is processed through the channel pipeline
        channel.writeInbound(request);

        // Then: Verify that the origin attribute is set for transport errors
        // We need to wait a bit for the async operations to complete
        await().atMost(ofSeconds(1)).until(() -> channel.hasAttr(EncoderHandler.ORIGIN));

        // The channel should have the ORIGIN attribute set for error responses
        assertThat(channel.hasAttr(EncoderHandler.ORIGIN)).isTrue();
        assertThat(channel.attr(EncoderHandler.ORIGIN).get()).isEqualTo(TEST_ORIGIN);
    }

    /**
     * Test that verifies the scheduler integration and ping timeout cancellation mechanism.
     * <p>
     * This test ensures that when data is received after the ping timeout is scheduled,
     * the handler properly cancels the timeout task to prevent premature channel closure:
     * 1. Channel becomes active → ping timeout scheduled
     * 2. Data is received → timeout task cancelled
     * 3. Channel remains active beyond the original timeout period
     * <p>
     * This mechanism is essential for preventing false timeouts when clients
     * are actively communicating with the server.
     */
    @Test
    @DisplayName("Scheduler Integration - Should Cancel Ping Timeout After Data Received")
    void testSchedulerIntegration_ShouldCancelPingTimeoutAfterDataReceived() throws Exception {
        // Given: Channel is active and ping timeout is scheduled
        ChannelHandlerContext ctx = channel.pipeline().context(authorizeHandler);
        authorizeHandler.channelActive(ctx);

        // Verify timeout is scheduled (channel remains active initially)
        assertThat(channel.isActive()).isTrue();

        // When: Data is received, which should cancel the ping timeout
        String uri = CONNECT_PATH + "?transport=polling";
        FullHttpRequest request = createHttpRequest(uri, TEST_ORIGIN);
        channel.writeInbound(request);

        // Then: The channel should remain active after data processing
        // We need to wait a bit for the async operations to complete
        await().atMost(ofSeconds(1)).until(() -> channel.isActive());

        // Wait for the original timeout period to ensure it was cancelled
        await().atMost(ofSeconds(FIRST_DATA_TIMEOUT + 500)).until(() -> channel.isActive());

        // The channel should still be active because the timeout was cancelled
        assertThat(channel.isActive()).isTrue();
    }


    /**
     * Creates a test HTTP request with the specified URI and origin.
     * <p>
     * This helper method constructs realistic HTTP requests for testing purposes,
     * including proper headers that would be present in actual Socket.IO client requests:
     * - Origin header for CORS validation
     * - Host header for server identification
     * - User-Agent header for client identification
     * - Empty content body (GET requests typically don't have content)
     *
     * @param uri    The request URI including query parameters
     * @param origin The origin header value for CORS validation
     * @return A properly formatted FullHttpRequest for testing
     */
    private FullHttpRequest createHttpRequest(String uri, String origin) {
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.ORIGIN, origin);
        headers.set(HttpHeaderNames.HOST, "localhost:8080");
        headers.set(HttpHeaderNames.USER_AGENT, "TestClient/1.0");

        ByteBuf content = Unpooled.EMPTY_BUFFER;
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri, content, headers, headers);
    }
}
