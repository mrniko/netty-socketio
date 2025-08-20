package com.corundumstudio.socketio.store;

import com.corundumstudio.socketio.store.CustomizedRedisContainer;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RedissonStoreFactory using testcontainers
 */
public class RedissonStoreFactoryTest extends StoreFactoryTest {

    private GenericContainer<?> container;
    private RedissonClient redissonClient;

    @Override
    protected StoreFactory createStoreFactory() throws Exception {
        container = new CustomizedRedisContainer();
        container.start();
        
        CustomizedRedisContainer customizedRedisContainer = (CustomizedRedisContainer) container;
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + customizedRedisContainer.getHost() + ":" + customizedRedisContainer.getRedisPort());
        
        redissonClient = Redisson.create(config);
        return new RedissonStoreFactory(redissonClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (storeFactory != null) {
            storeFactory.shutdown();
        }
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    @Test
    public void testRedissonSpecificFeatures() {
        // Test that the factory creates Redisson-specific stores
        UUID sessionId = UUID.randomUUID();
        Store store = storeFactory.createStore(sessionId);
        
        assertNotNull(store, "Store should not be null");
        assertTrue(store instanceof RedissonStore, "Store should be RedissonStore");
        
        // Test that the store works with Redisson
        store.set("redissonKey", "redissonValue");
        assertEquals("redissonValue", store.get("redissonKey"));
    }

    @Test
    public void testRedissonPubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull(pubSubStore, "PubSubStore should not be null");
        assertTrue(pubSubStore instanceof RedissonPubSubStore, "PubSubStore should be RedissonPubSubStore");
    }

    @Test
    public void testRedissonMapCreation() {
        String mapName = "testRedissonMap";
        java.util.Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull(map, "Map should not be null");
        assertTrue(map instanceof java.util.Map, "Map should implement Map interface");
        
        // Test that the map works
        map.put("testKey", "testValue");
        assertEquals("testValue", map.get("testKey"));
    }
}
