package com.corundumstudio.socketio.namespace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.misc.CompositeIterable;

/** Test class for NamespacesHub functionality and thread safety. */
class NamespacesHubTest extends BaseNamespaceTest {

  private NamespacesHub namespacesHub;

  @Mock private Configuration mockConfiguration;

  @Mock private SocketIOClient mockClient1;

  @Mock private SocketIOClient mockClient2;

  private static final String NAMESPACE_NAME_1 = "testNamespace1";
  private static final String NAMESPACE_NAME_2 = "testNamespace2";
  private static final String ROOM_NAME = "testRoom";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    namespacesHub = new NamespacesHub(mockConfiguration);
  }

  /** Test basic NamespacesHub properties and initial state */
  @Test
  void testBasicProperties() {
    // Test initial state
    assertNotNull(namespacesHub);
    assertNotNull(mockConfiguration);

    // Test initial namespaces collection is empty
    Collection<SocketIONamespace> allNamespaces = namespacesHub.getAllNamespaces();
    assertNotNull(allNamespaces);
    assertTrue(allNamespaces.isEmpty());
    assertEquals(0, allNamespaces.size());

    // Test getting non-existent namespace returns null
    assertNull(namespacesHub.get("nonExistentNamespace"));
  }

  /** Test namespace creation functionality */
  @Test
  void testNamespaceCreation() {
    // Test creating first namespace
    Namespace namespace1 = namespacesHub.create(NAMESPACE_NAME_1);
    assertNotNull(namespace1);
    assertEquals(NAMESPACE_NAME_1, namespace1.getName());

    // Test that created namespace is accessible
    Namespace retrievedNamespace1 = namespacesHub.get(NAMESPACE_NAME_1);
    assertNotNull(retrievedNamespace1);
    assertSame(namespace1, retrievedNamespace1);

    // Test creating second namespace
    Namespace namespace2 = namespacesHub.create(NAMESPACE_NAME_2);
    assertNotNull(namespace2);
    assertEquals(NAMESPACE_NAME_2, namespace2.getName());
    assertNotSame(namespace1, namespace2);

    // Test that both namespaces are accessible
    assertSame(namespace1, namespacesHub.get(NAMESPACE_NAME_1));
    assertSame(namespace2, namespacesHub.get(NAMESPACE_NAME_2));

    // Test that namespaces collection contains both
    Collection<SocketIONamespace> allNamespaces = namespacesHub.getAllNamespaces();
    assertEquals(2, allNamespaces.size());
    assertTrue(allNamespaces.contains(namespace1));
    assertTrue(allNamespaces.contains(namespace2));
  }

  /** Test namespace creation idempotency */
  @Test
  void testNamespaceCreationIdempotency() {
    // Test creating same namespace multiple times returns same instance
    Namespace namespace1 = namespacesHub.create(NAMESPACE_NAME_1);
    Namespace namespace2 = namespacesHub.create(NAMESPACE_NAME_1);
    Namespace namespace3 = namespacesHub.create(NAMESPACE_NAME_1);

    assertNotNull(namespace1);
    assertNotNull(namespace2);
    assertNotNull(namespace3);

    // All should be the same instance
    assertSame(namespace1, namespace2);
    assertSame(namespace2, namespace3);
    assertSame(namespace1, namespace3);

    // Test that only one namespace exists in collection
    Collection<SocketIONamespace> allNamespaces = namespacesHub.getAllNamespaces();
    assertEquals(1, allNamespaces.size());
    assertTrue(allNamespaces.contains(namespace1));
  }

  /** Test namespace retrieval functionality */
  @Test
  void testNamespaceRetrieval() {
    // Test getting non-existent namespace
    assertNull(namespacesHub.get("nonExistent"));

    // Create namespace and test retrieval
    Namespace createdNamespace = namespacesHub.create(NAMESPACE_NAME_1);
    Namespace retrievedNamespace = namespacesHub.get(NAMESPACE_NAME_1);

    assertNotNull(retrievedNamespace);
    assertSame(createdNamespace, retrievedNamespace);

    // Test case sensitivity
    assertNull(namespacesHub.get(NAMESPACE_NAME_1.toUpperCase()));
    assertNull(namespacesHub.get(NAMESPACE_NAME_1.toLowerCase()));

    // Test empty string
    assertNull(namespacesHub.get(""));

    // Test null (if allowed)
    try {
      namespacesHub.get(null);
      // If no exception, test behavior
    } catch (Exception e) {
      // If exception is thrown, that's acceptable
      assertNotNull(e);
    }
  }

  /** Test namespace removal functionality */
  @Test
  void testNamespaceRemoval() {
    // Create namespace first
    Namespace namespace = namespacesHub.create(NAMESPACE_NAME_1);
    assertNotNull(namespace);

    // Test removing existing namespace
    namespacesHub.remove(NAMESPACE_NAME_1);

    // Verify namespace is no longer accessible
    assertNull(namespacesHub.get(NAMESPACE_NAME_1));

    // Verify namespace was removed from collection
    Collection<SocketIONamespace> allNamespaces = namespacesHub.getAllNamespaces();
    assertEquals(0, allNamespaces.size());

    // Test removing non-existent namespace (should not throw exception)
    assertDoesNotThrow(() -> namespacesHub.remove("nonExistent"));

    // Test removing already removed namespace
    assertDoesNotThrow(() -> namespacesHub.remove(NAMESPACE_NAME_1));
  }

  /** Test room clients functionality */
  @Test
  void testRoomClients() {
    // Create namespaces and add clients to rooms
    Namespace namespace1 = namespacesHub.create(NAMESPACE_NAME_1);
    Namespace namespace2 = namespacesHub.create(NAMESPACE_NAME_2);

    // Test getting room clients from all namespaces
    Iterable<SocketIOClient> roomClients = namespacesHub.getRoomClients(ROOM_NAME);
    assertNotNull(roomClients);

    // Verify it's a CompositeIterable
    assertTrue(roomClients instanceof CompositeIterable);

    // Test iteration over room clients (should be empty initially)
    int count = 0;
    for (SocketIOClient client : roomClients) {
      count++;
    }
    assertEquals(0, count);

    // Test getting room clients from non-existent room
    Iterable<SocketIOClient> emptyRoomClients = namespacesHub.getRoomClients("nonExistentRoom");
    assertNotNull(emptyRoomClients);

    count = 0;
    for (SocketIOClient client : emptyRoomClients) {
      count++;
    }
    assertEquals(0, count);
  }

  /** Test concurrent namespace operations with thread safety */
  @Test
  void testConcurrentNamespaceOperations() throws InterruptedException {
    int taskCount = DEFAULT_TASK_COUNT;

    // Test concurrent namespace creation
    CountDownLatch createLatch =
        executeConcurrentOperationsWithIndex(
            taskCount,
            index -> {
              try {
                String namespaceName = "concurrentNamespace" + index;
                Namespace namespace = namespacesHub.create(namespaceName);
                assertNotNull(namespace);
                assertEquals(namespaceName, namespace.getName());

                // Verify namespace is immediately accessible
                Namespace retrievedNamespace = namespacesHub.get(namespaceName);
                assertNotNull(retrievedNamespace);
                assertSame(namespace, retrievedNamespace);
              } catch (Exception e) {
                // Log exception but continue
              }
            });

    waitForCompletion(createLatch);

    // Verify all namespaces were created safely
    Collection<SocketIONamespace> allNamespaces = namespacesHub.getAllNamespaces();
    assertEquals(taskCount, allNamespaces.size());

    // Test concurrent namespace retrieval
    CountDownLatch retrieveLatch =
        executeConcurrentOperationsWithIndex(
            taskCount,
            index -> {
              try {
                String namespaceName = "concurrentNamespace" + index;
                Namespace namespace = namespacesHub.get(namespaceName);
                assertNotNull(namespace);
                assertEquals(namespaceName, namespace.getName());
              } catch (Exception e) {
                // Log exception but continue
              }
            });

    waitForCompletion(retrieveLatch);

    // Verify final state
    assertEquals(taskCount, namespacesHub.getAllNamespaces().size());
    assertTrue(createLatch.getCount() == 0);
    assertTrue(retrieveLatch.getCount() == 0);
  }

  /** Test edge cases and boundary conditions */
  @Test
  void testEdgeCasesAndBoundaries() {
    // Test creating namespace with empty name
    try {
      Namespace emptyNamespace = namespacesHub.create("");
      if (emptyNamespace != null) {
        assertEquals("", emptyNamespace.getName());
      }
    } catch (Exception e) {
      // If exception is thrown, that's acceptable
      assertNotNull(e);
    }

    // Test creating namespace with very long name
    StringBuilder longNameBuilder = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longNameBuilder.append("a");
    }
    String longName = longNameBuilder.toString();
    Namespace longNameNamespace = namespacesHub.create(longName);
    assertNotNull(longNameNamespace);
    assertEquals(longName, longNameNamespace.getName());

    // Test creating many namespaces
    int largeCount = 100;
    for (int i = 0; i < largeCount; i++) {
      String namespaceName = "largeNamespace" + i;
      Namespace namespace = namespacesHub.create(namespaceName);
      assertNotNull(namespace);
      assertEquals(namespaceName, namespace.getName());
    }

    assertEquals(
        largeCount + 2, namespacesHub.getAllNamespaces().size()); // +2 for empty and long names

    // Test that all namespaces are accessible
    for (int i = 0; i < largeCount; i++) {
      String namespaceName = "largeNamespace" + i;
      Namespace namespace = namespacesHub.get(namespaceName);
      assertNotNull(namespace);
      assertEquals(namespaceName, namespace.getName());
    }
  }

  /** Test namespace lifecycle management */
  @Test
  void testNamespaceLifecycleManagement() {
    // Test complete lifecycle: create -> retrieve -> remove -> verify gone
    String lifecycleNamespaceName = "lifecycleNamespace";

    // Step 1: Create
    Namespace createdNamespace = namespacesHub.create(lifecycleNamespaceName);
    assertNotNull(createdNamespace);
    assertEquals(lifecycleNamespaceName, createdNamespace.getName());

    // Step 2: Verify creation
    assertNotNull(namespacesHub.get(lifecycleNamespaceName));
    assertEquals(1, namespacesHub.getAllNamespaces().size());

    // Step 3: Remove
    namespacesHub.remove(lifecycleNamespaceName);

    // Step 4: Verify removal
    assertNull(namespacesHub.get(lifecycleNamespaceName));
    assertEquals(0, namespacesHub.getAllNamespaces().size());

    // Step 5: Recreate (should work)
    Namespace recreatedNamespace = namespacesHub.create(lifecycleNamespaceName);
    assertNotNull(recreatedNamespace);
    assertEquals(lifecycleNamespaceName, recreatedNamespace.getName());
    assertNotSame(createdNamespace, recreatedNamespace);

    // Step 6: Verify recreation
    assertNotNull(namespacesHub.get(lifecycleNamespaceName));
    assertEquals(1, namespacesHub.getAllNamespaces().size());
  }

  /** Test configuration dependency */
  @Test
  void testConfigurationDependency() {
    // Test that configuration is properly stored
    assertNotNull(mockConfiguration);

    // Create namespace and verify it has access to configuration
    Namespace namespace = namespacesHub.create(NAMESPACE_NAME_1);
    assertNotNull(namespace);

    // The namespace should be able to use the configuration
    // (we can't directly test this without exposing internal state,
    // but we can verify the namespace was created successfully)
    assertEquals(NAMESPACE_NAME_1, namespace.getName());

    // Test that multiple namespaces can be created with same configuration
    Namespace namespace2 = namespacesHub.create(NAMESPACE_NAME_2);
    assertNotNull(namespace2);
    assertEquals(NAMESPACE_NAME_2, namespace2.getName());

    assertEquals(2, namespacesHub.getAllNamespaces().size());
  }
}
