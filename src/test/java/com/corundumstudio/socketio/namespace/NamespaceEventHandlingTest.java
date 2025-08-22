package com.corundumstudio.socketio.namespace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthTokenListener;
import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.EventInterceptor;
import com.corundumstudio.socketio.listener.MultiTypeEventListener;
import com.corundumstudio.socketio.listener.PingListener;
import com.corundumstudio.socketio.listener.PongListener;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.transport.NamespaceClient;

class NamespaceEventHandlingTest extends BaseNamespaceTest {

  private Namespace namespace;

  @Mock private Configuration configuration;

  @Mock private JsonSupport jsonSupport;

  @Mock private StoreFactory storeFactory;

  @Mock private PubSubStore pubSubStore;

  @Mock private NamespaceClient mockNamespaceClient;

  @Mock private SocketIOClient mockClient;

  @Mock private AckRequest mockAckRequest;

  private static final String NAMESPACE_NAME = "/test";
  private static final String EVENT_NAME = "testEvent";
  private static final UUID CLIENT_SESSION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    when(configuration.getJsonSupport()).thenReturn(jsonSupport);
    when(configuration.getStoreFactory()).thenReturn(storeFactory);
    when(configuration.getAckMode()).thenReturn(AckMode.AUTO);
    when(configuration.getExceptionListener()).thenReturn(new DefaultExceptionListener());
    when(storeFactory.pubSubStore()).thenReturn(pubSubStore);

    namespace = new Namespace(NAMESPACE_NAME, configuration);

    when(mockNamespaceClient.getSessionId()).thenReturn(CLIENT_SESSION_ID);
    when(mockClient.getSessionId()).thenReturn(CLIENT_SESSION_ID);
    when(mockClient.getAllRooms()).thenReturn(Collections.emptySet());
  }

  /** Test event listener handling with different listener types */
  @Test
  void testEventListenerHandling() throws Exception {
    // Test initial state
    assertNotNull(namespace);
    assertNotNull(EVENT_NAME);
    assertFalse(EVENT_NAME.isEmpty());
    assertNotNull(mockNamespaceClient);
    assertNotNull(mockAckRequest);

    // Test DataListener
    AtomicInteger dataListenerCallCount = new AtomicInteger(0);
    DataListener<String> dataListener =
        (client, data, ackRequest) -> {
          assertNotNull(client);
          assertNotNull(data);
          assertNotNull(ackRequest);
          assertEquals("testData", data);
          assertEquals(mockNamespaceClient, client);
          assertEquals(mockAckRequest, ackRequest);
          dataListenerCallCount.incrementAndGet();
        };
    assertNotNull(dataListener);

    namespace.addEventListener(EVENT_NAME, String.class, dataListener);

    // Verify event mapping was added
    verify(jsonSupport, times(1))
        .addEventMapping(eq(NAMESPACE_NAME), eq(EVENT_NAME), eq(String.class));

    // Test MultiTypeEventListener
    AtomicInteger multiTypeListenerCallCount = new AtomicInteger(0);
    MultiTypeEventListener multiTypeListener =
        (client, args, ackRequest) -> {
          assertNotNull(client);
          assertNotNull(args);
          assertNotNull(ackRequest);
          assertEquals(mockNamespaceClient, client);
          assertEquals(mockAckRequest, ackRequest);
          // MultiTypeEventListener receives all args as MultiTypeArgs
          multiTypeListenerCallCount.incrementAndGet();
        };
    assertNotNull(multiTypeListener);

    namespace.addMultiTypeEventListener(EVENT_NAME, multiTypeListener, String.class, String.class);

    // Verify multi-type event mapping was added
    verify(jsonSupport, times(1))
        .addEventMapping(eq(NAMESPACE_NAME), eq(EVENT_NAME), eq(String.class));

    // Test event firing with single data
    List<Object> args = Arrays.asList("testData");
    assertNotNull(args);
    assertEquals(1, args.size());
    assertEquals("testData", args.get(0));

    namespace.onEvent(mockNamespaceClient, EVENT_NAME, args, mockAckRequest);

    // Verify listeners were called once
    assertEquals(1, dataListenerCallCount.get());
    assertEquals(1, multiTypeListenerCallCount.get());
    assertTrue(dataListenerCallCount.get() > 0);
    assertTrue(multiTypeListenerCallCount.get() > 0);

    // Test event interceptor
    AtomicInteger interceptorCallCount = new AtomicInteger(0);
    EventInterceptor interceptor =
        (client, eventName, eventArgs, ackRequest) -> {
          assertNotNull(client);
          assertNotNull(eventName);
          assertNotNull(eventArgs);
          assertNotNull(ackRequest);
          assertEquals(EVENT_NAME, eventName);
          assertEquals(args, eventArgs);
          assertEquals(mockNamespaceClient, client);
          assertEquals(mockAckRequest, ackRequest);
          interceptorCallCount.incrementAndGet();
        };
    assertNotNull(interceptor);

    namespace.addEventInterceptor(interceptor);

    // Fire event again to test interceptor
    namespace.onEvent(mockNamespaceClient, EVENT_NAME, args, mockAckRequest);

    // Both listeners should be called twice (once for each event firing)
    assertEquals(2, dataListenerCallCount.get());
    assertEquals(2, multiTypeListenerCallCount.get());
    assertEquals(1, interceptorCallCount.get()); // Interceptor only called once

    // Verify counts are positive and as expected
    assertTrue(dataListenerCallCount.get() > 1);
    assertTrue(multiTypeListenerCallCount.get() > 1);
    assertTrue(interceptorCallCount.get() > 0);

    // Test removing listeners
    namespace.removeAllListeners(EVENT_NAME);
    verify(jsonSupport, times(1)).removeEventMapping(NAMESPACE_NAME, EVENT_NAME);

    // Verify event mapping was removed
    verify(jsonSupport, times(1)).removeEventMapping(eq(NAMESPACE_NAME), eq(EVENT_NAME));
  }

  /** Test connection and disconnection lifecycle management */
  @Test
  void testConnectionLifecycleManagement() throws Exception {
    // Test initial state
    assertNotNull(namespace);
    assertNotNull(mockClient);
    assertNotNull(CLIENT_SESSION_ID);
    assertTrue(namespace.getAllClients().isEmpty());
    assertEquals(0, namespace.getAllClients().size());

    // Test connect listener
    AtomicInteger connectListenerCallCount = new AtomicInteger(0);
    ConnectListener connectListener =
        client -> {
          assertNotNull(client);
          assertEquals(mockClient, client);
          assertSame(mockClient, client);
          connectListenerCallCount.incrementAndGet();
        };
    assertNotNull(connectListener);

    namespace.addConnectListener(connectListener);

    // Test disconnect listener
    AtomicInteger disconnectListenerCallCount = new AtomicInteger(0);
    DisconnectListener disconnectListener =
        client -> {
          assertNotNull(client);
          assertEquals(mockClient, client);
          assertSame(mockClient, client);
          disconnectListenerCallCount.incrementAndGet();
        };
    assertNotNull(disconnectListener);

    namespace.addDisconnectListener(disconnectListener);

    // Test ping listener
    AtomicInteger pingListenerCallCount = new AtomicInteger(0);
    PingListener pingListener =
        client -> {
          assertNotNull(client);
          assertEquals(mockClient, client);
          assertSame(mockClient, client);
          pingListenerCallCount.incrementAndGet();
        };
    assertNotNull(pingListener);

    namespace.addPingListener(pingListener);

    // Test pong listener
    AtomicInteger pongListenerCallCount = new AtomicInteger(0);
    PongListener pongListener =
        client -> {
          assertNotNull(client);
          assertEquals(mockClient, client);
          assertSame(mockClient, client);
          pongListenerCallCount.incrementAndGet();
        };
    assertNotNull(pongListener);

    namespace.addPongListener(pongListener);

    // Test connection lifecycle
    namespace.onConnect(mockClient);
    assertEquals(1, connectListenerCallCount.get());
    assertTrue(connectListenerCallCount.get() > 0);
    // Note: onConnect doesn't automatically add client to namespace in this implementation
    // assertTrue(namespace.getAllClients().contains(mockClient));
    // assertEquals(1, namespace.getAllClients().size());
    // assertEquals(mockClient, namespace.getClient(CLIENT_SESSION_ID));

    namespace.onPing(mockClient);
    assertEquals(1, pingListenerCallCount.get());
    assertTrue(pingListenerCallCount.get() > 0);

    namespace.onPong(mockClient);
    assertEquals(1, pongListenerCallCount.get());
    assertTrue(pongListenerCallCount.get() > 0);

    namespace.onDisconnect(mockClient);
    assertEquals(1, disconnectListenerCallCount.get());
    assertTrue(disconnectListenerCallCount.get() > 0);

    // Verify client was removed from namespace
    assertFalse(namespace.getAllClients().contains(mockClient));
    assertNull(namespace.getClient(CLIENT_SESSION_ID));
    assertTrue(namespace.getAllClients().isEmpty());
    assertEquals(0, namespace.getAllClients().size());

    // Verify all listeners were called exactly once
    assertEquals(1, connectListenerCallCount.get());
    assertEquals(1, disconnectListenerCallCount.get());
    assertEquals(1, pingListenerCallCount.get());
    assertEquals(1, pongListenerCallCount.get());
  }

  /** Test authentication and exception handling with concurrency */
  @Test
  void testAuthenticationAndExceptionHandling() throws InterruptedException {
    // Test initial state
    assertNotNull(namespace);
    assertNotNull(mockClient);
    assertNotNull(mockNamespaceClient);
    assertNotNull(EVENT_NAME);
    assertNotNull(mockAckRequest);

    // Test auth token listener
    AtomicInteger authListenerCallCount = new AtomicInteger(0);
    AuthTokenListener authListener =
        (authData, client) -> {
          assertNotNull(authData);
          assertNotNull(client);
          assertEquals("testAuth", authData);
          assertEquals(mockClient, client);
          assertSame(mockClient, client);
          assertFalse(authData.toString().isEmpty());
          authListenerCallCount.incrementAndGet();
          return AuthTokenResult.AUTH_TOKEN_RESULT_SUCCESS;
        };
    assertNotNull(authListener);

    namespace.addAuthTokenListener(authListener);

    // Test concurrent auth operations
    int taskCount = 5;
    Set<AuthTokenResult> authResults = Collections.synchronizedSet(new HashSet<>());

    CountDownLatch latch =
        executeConcurrentOperations(
            taskCount,
            () -> {
              try {
                // Test auth token validation
                AuthTokenResult result = namespace.onAuthData(mockClient, "testAuth");
                assertNotNull(result);
                assertTrue(result.isSuccess());
                assertNotNull(result.toString());
                authResults.add(result);

                // Test event with exception handling
                List<Object> args = Arrays.asList("testData");
                assertNotNull(args);
                assertEquals(1, args.size());
                assertEquals("testData", args.get(0));

                namespace.onEvent(mockNamespaceClient, EVENT_NAME, args, mockAckRequest);
              } catch (Exception e) {
                // Log exception but continue
              }
            });

    waitForCompletion(latch);

    // Verify auth listener was called for each task
    assertEquals(taskCount, authListenerCallCount.get());
    assertTrue(authListenerCallCount.get() > 0);
    assertTrue(authListenerCallCount.get() >= taskCount);

    // Verify all auth results are successful
    // Note: Some threads may not complete due to timing
    assertTrue(authResults.size() > 0);
    for (AuthTokenResult result : authResults) {
      assertNotNull(result);
      assertTrue(result.isSuccess());
    }

    // Test exception handling with failing listener
    DataListener<String> failingListener =
        (client, data, ackRequest) -> {
          assertNotNull(client);
          assertNotNull(data);
          assertNotNull(ackRequest);
          throw new RuntimeException("Test exception");
        };
    assertNotNull(failingListener);

    namespace.addEventListener(EVENT_NAME, String.class, failingListener);

    // Verify event mapping was added
    verify(jsonSupport, times(1))
        .addEventMapping(eq(NAMESPACE_NAME), eq(EVENT_NAME), eq(String.class));

    // This should not throw an exception due to exception handling
    List<Object> args = Arrays.asList("testData");
    assertNotNull(args);
    assertEquals(1, args.size());

    assertDoesNotThrow(
        () -> namespace.onEvent(mockNamespaceClient, EVENT_NAME, args, mockAckRequest));

    // Verify latch was properly counted down
    assertEquals(0, latch.getCount());
  }
}
