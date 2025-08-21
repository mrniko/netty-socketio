package com.corundumstudio.socketio.store;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

/** Test class for RedissonStore using testcontainers */
public class RedissonStoreTest extends AbstractStoreTest {

  private RedissonClient redissonClient;

  @Override
  protected GenericContainer<?> createContainer() {
    return new CustomizedRedisContainer();
  }

  @Override
  protected Store createStore(UUID sessionId) throws Exception {
    CustomizedRedisContainer customizedRedisContainer = (CustomizedRedisContainer) container;
    Config config = new Config();
    config
        .useSingleServer()
        .setAddress(
            "redis://"
                + customizedRedisContainer.getHost()
                + ":"
                + customizedRedisContainer.getRedisPort());

    redissonClient = Redisson.create(config);
    return new RedissonStore(sessionId, redissonClient);
  }

  @Override
  protected void cleanupStore() {
    if (redissonClient != null) {
      redissonClient.shutdown();
    }
  }

  @Test
  public void testRedissonSpecificFeatures() {
    // Test that the store is actually using Redisson
    assertNotNull(store);

    // Test Redis-specific features like TTL (if supported)
    store.set("ttlKey", "ttlValue");
    assertEquals("ttlValue", store.get("ttlKey"));
  }

  @Test
  public void testRedisDataPersistence() {
    // Test that data persists across operations
    store.set("persistentKey", "persistentValue");
    assertEquals("persistentValue", store.get("persistentKey"));

    // Verify the key exists
    assertTrue(store.has("persistentKey"));

    // Delete and verify it's gone
    store.del("persistentKey");
    assertFalse(store.has("persistentKey"));
    assertNull(store.get("persistentKey"));
  }
}
