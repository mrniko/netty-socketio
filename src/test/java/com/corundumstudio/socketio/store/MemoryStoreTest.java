package com.corundumstudio.socketio.store;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

/** Test class for MemoryStore - no container needed as it's in-memory */
public class MemoryStoreTest extends AbstractStoreTest {

  @Override
  protected GenericContainer<?> createContainer() {
    // Memory store doesn't need a container
    return null;
  }

  @Override
  protected Store createStore(UUID sessionId) throws Exception {
    return new MemoryStore();
  }

  @Override
  protected void cleanupStore() {
    // Memory store cleanup is automatic
  }

  @Override
  public void setUp() throws Exception {
    sessionId = UUID.randomUUID();
    store = createStore(sessionId);
  }

  @Override
  public void tearDown() throws Exception {
    if (store != null) {
      cleanupStore();
    }
  }

  @Test
  public void testMemoryStoreSpecificFeatures() {
    // Test that the store is actually using memory storage
    assertNotNull(store);

    // Test that data is immediately available
    store.set("immediateKey", "immediateValue");
    assertEquals("immediateValue", store.get("immediateKey"));

    // Test that data is not shared between different stores
    Store anotherStore = new MemoryStore();
    anotherStore.set("sharedKey", "sharedValue");

    // The original store should not have this key
    assertFalse(store.has("sharedKey"));
    assertNull(store.get("sharedKey"));
  }

  @Test
  public void testMemoryStoreIsolation() {
    // Create two different stores with different session IDs
    Store store1 = new MemoryStore();
    Store store2 = new MemoryStore();

    // Set data in store1
    store1.set("isolatedKey", "store1Value");

    // Store2 should not have this data
    assertFalse(store2.has("isolatedKey"));
    assertNull(store2.get("isolatedKey"));

    // Store1 should still have the data
    assertTrue(store1.has("isolatedKey"));
    assertEquals("store1Value", store1.get("isolatedKey"));
  }

  @Test
  public void testMemoryStorePerformance() {
    // Test performance with many operations
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 10000; i++) {
      store.set("perfKey" + i, "perfValue" + i);
    }

    long setTime = System.currentTimeMillis() - startTime;

    startTime = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      store.get("perfKey" + i);
    }

    long getTime = System.currentTimeMillis() - startTime;

    // Memory operations should be very fast
    assertTrue(setTime < 1000, "Set operations took too long: " + setTime + "ms");
    assertTrue(getTime < 1000, "Get operations took too long: " + getTime + "ms");
  }
}
