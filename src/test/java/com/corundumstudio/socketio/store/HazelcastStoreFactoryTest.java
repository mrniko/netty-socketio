package com.corundumstudio.socketio.store;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

/** Test class for HazelcastStoreFactory using testcontainers */
public class HazelcastStoreFactoryTest extends StoreFactoryTest {

  private GenericContainer<?> container;
  private HazelcastInstance hazelcastInstance;

  @Override
  protected StoreFactory createStoreFactory() throws Exception {
    container = new CustomizedHazelcastContainer();
    container.start();

    CustomizedHazelcastContainer customizedHazelcastContainer =
        (CustomizedHazelcastContainer) container;
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
    clientConfig
        .getNetworkConfig()
        .addAddress(
            customizedHazelcastContainer.getHost()
                + ":"
                + customizedHazelcastContainer.getHazelcastPort());

    hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
    return new HazelcastStoreFactory(hazelcastInstance);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (storeFactory != null) {
      storeFactory.shutdown();
    }
    if (hazelcastInstance != null) {
      hazelcastInstance.shutdown();
    }
    if (container != null && container.isRunning()) {
      container.stop();
    }
  }

  @Test
  public void testHazelcastSpecificFeatures() {
    // Test that the factory creates Hazelcast-specific stores
    UUID sessionId = UUID.randomUUID();
    Store store = storeFactory.createStore(sessionId);

    assertNotNull(store, "Store should not be null");
    assertTrue(store instanceof HazelcastStore, "Store should be HazelcastStore");

    // Test that the store works with Hazelcast
    store.set("hazelcastKey", "hazelcastValue");
    assertEquals("hazelcastValue", store.get("hazelcastKey"));
  }

  @Test
  public void testHazelcastPubSubStore() {
    PubSubStore pubSubStore = storeFactory.pubSubStore();

    assertNotNull(pubSubStore, "PubSubStore should not be null");
    assertTrue(pubSubStore instanceof HazelcastPubSubStore, "PubSubStore should be HazelcastStore");
  }

  @Test
  public void testHazelcastMapCreation() {
    String mapName = "testHazelcastMap";
    java.util.Map<String, Object> map = storeFactory.createMap(mapName);

    assertNotNull(map, "Map should not be null");
    assertTrue(map instanceof java.util.Map, "Map should implement Map interface");

    // Test that the map works
    map.put("testKey", "testValue");
    assertEquals("testValue", map.get("testKey"));
  }
}
