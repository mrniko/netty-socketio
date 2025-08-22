package com.corundumstudio.socketio.namespace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.transport.NamespaceClient;

class NamespaceTest extends BaseNamespaceTest {

  private Namespace namespace;

  @Mock private Configuration configuration;

  @Mock private JsonSupport jsonSupport;

  @Mock private StoreFactory storeFactory;

  @Mock private SocketIOClient mockClient;

  @Mock private NamespaceClient mockNamespaceClient;

  private static final String NAMESPACE_NAME = "/test";
  private static final UUID CLIENT_SESSION_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    when(configuration.getJsonSupport()).thenReturn(jsonSupport);
    when(configuration.getStoreFactory()).thenReturn(storeFactory);
    when(configuration.getAckMode()).thenReturn(AckMode.AUTO);
    when(configuration.getExceptionListener()).thenReturn(new DefaultExceptionListener());

    namespace = new Namespace(NAMESPACE_NAME, configuration);

    when(mockClient.getSessionId()).thenReturn(CLIENT_SESSION_ID);
    when(mockClient.getAllRooms()).thenReturn(Collections.emptySet());
    when(mockNamespaceClient.getSessionId()).thenReturn(CLIENT_SESSION_ID);

    // Mock StoreFactory pubSubStore to avoid NullPointerException
    when(storeFactory.pubSubStore()).thenReturn(mock(PubSubStore.class));
  }

  /** Test basic namespace properties and initialization */
  @Test
  void testBasicProperties() {
    // Test namespace name
    assertEquals(NAMESPACE_NAME, namespace.getName());
    assertNotNull(namespace.getName());
    assertFalse(namespace.getName().isEmpty());

    // Test default namespace name constant
    assertEquals("", Namespace.DEFAULT_NAME);
    assertNotNull(Namespace.DEFAULT_NAME);

    // Test initial state
    assertTrue(namespace.getAllClients().isEmpty());
    assertEquals(0, namespace.getAllClients().size());
    assertTrue(namespace.getRooms().isEmpty());
    assertEquals(0, namespace.getRooms().size());
    assertNull(namespace.getClient(CLIENT_SESSION_ID));

    // Test namespace is not null
    assertNotNull(namespace);

    // Test namespace is properly initialized
    assertNotNull(namespace);
  }

  /** Test client management operations with concurrency safety */
  @Test
  void testClientManagement() throws InterruptedException {
    // Test initial state
    assertTrue(namespace.getAllClients().isEmpty());
    assertEquals(0, namespace.getAllClients().size());

    // Test adding client
    namespace.addClient(mockClient);
    assertEquals(1, namespace.getAllClients().size());
    assertTrue(namespace.getAllClients().contains(mockClient));
    assertEquals(mockClient, namespace.getClient(CLIENT_SESSION_ID));
    assertNotNull(namespace.getClient(CLIENT_SESSION_ID));

    // Verify client properties
    assertNotNull(mockClient.getSessionId());
    assertEquals(CLIENT_SESSION_ID, mockClient.getSessionId());

    // Test concurrent client addition
    int taskCount = DEFAULT_TASK_COUNT;
    Set<UUID> addedSessionIds = Collections.synchronizedSet(new HashSet<>());

    CountDownLatch latch =
        executeConcurrentOperationsWithIndex(
            taskCount,
            index -> {
              try {
                SocketIOClient client = mock(SocketIOClient.class);
                UUID sessionId = UUID.randomUUID();
                when(client.getSessionId()).thenReturn(sessionId);
                when(client.getAllRooms()).thenReturn(Collections.emptySet());

                namespace.addClient(client);
                addedSessionIds.add(sessionId);
              } catch (Exception e) {
                // Log exception but continue
              }
            });

    waitForCompletion(latch);

    // Verify all clients were added safely
    assertEquals(taskCount + 1, namespace.getAllClients().size());
    assertTrue(namespace.getAllClients().size() > taskCount);

    // Verify each added client can be retrieved
    for (UUID sessionId : addedSessionIds) {
      assertNotNull(namespace.getClient(sessionId));
    }

    // Test client removal
    namespace.onDisconnect(mockClient);
    assertEquals(taskCount, namespace.getAllClients().size());
    assertFalse(namespace.getAllClients().contains(mockClient));
    assertNull(namespace.getClient(CLIENT_SESSION_ID));

    // Verify remaining clients are still accessible
    assertFalse(namespace.getAllClients().isEmpty());
    assertEquals(taskCount, namespace.getAllClients().size());

    // Test that operations completed successfully
    assertTrue(latch.getCount() == 0);
  }

  /** Test event listener management with thread safety */
  @Test
  void testEventListenerManagement() throws InterruptedException {
    // Test initial state - no listeners
    assertNotNull(namespace);

    // Test adding event listener
    String eventName = "testEvent";
    DataListener<String> listener = (client, data, ackRequest) -> {};
    assertNotNull(listener);
    assertNotNull(eventName);
    assertFalse(eventName.isEmpty());

    namespace.addEventListener(eventName, String.class, listener);

    // Verify event mapping was added
    verify(jsonSupport, times(1))
        .addEventMapping(eq(NAMESPACE_NAME), eq(eventName), eq(String.class));

    // Test concurrent listener addition
    int taskCount = 5;
    Set<String> addedEventNames = Collections.synchronizedSet(new HashSet<>());

    CountDownLatch latch =
        executeConcurrentOperationsWithIndex(
            taskCount,
            index -> {
              try {
                String concurrentEventName = "concurrentEvent" + index;
                DataListener<String> concurrentListener = (client, data, ackRequest) -> {};
                assertNotNull(concurrentListener);

                namespace.addEventListener(concurrentEventName, String.class, concurrentListener);
                addedEventNames.add(concurrentEventName);
              } catch (Exception e) {
                // Log exception but continue
              }
            });

    waitForCompletion(latch);

    // Verify all listeners were added safely
    verify(jsonSupport, times(taskCount + 1))
        .addEventMapping(eq(NAMESPACE_NAME), anyString(), eq(String.class));

    // Verify specific event names were processed
    for (String addedEventName : addedEventNames) {
      assertNotNull(addedEventName);
      assertFalse(addedEventName.isEmpty());
    }

    // Verify that operations completed successfully
    assertTrue(latch.getCount() == 0);

    // Test removing specific listener
    namespace.removeAllListeners(eventName);
    verify(jsonSupport).removeEventMapping(NAMESPACE_NAME, eventName);

    // Verify specific event mapping was removed
    verify(jsonSupport, times(1)).removeEventMapping(eq(NAMESPACE_NAME), eq(eventName));
  }
}
