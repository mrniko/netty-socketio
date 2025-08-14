package com.corundumstudio.socketio.store;

import com.corundumstudio.socketio.store.CustomizedRedisContainer;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.After;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.junit.Assert.*;

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

    @After
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
        
        assertNotNull("Store should not be null", store);
        assertTrue("Store should be RedissonStore", store instanceof RedissonStore);
        
        // Test that the store works with Redisson
        store.set("redissonKey", "redissonValue");
        assertEquals("redissonValue", store.get("redissonKey"));
    }

    @Test
    public void testRedissonPubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull("PubSubStore should not be null", pubSubStore);
        assertTrue("PubSubStore should be RedissonPubSubStore", pubSubStore instanceof RedissonPubSubStore);
    }

    @Test
    public void testRedissonMapCreation() {
        String mapName = "testRedissonMap";
        java.util.Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull("Map should not be null", map);
        assertTrue("Map should implement Map interface", map instanceof java.util.Map);
        
        // Test that the map works
        map.put("testKey", "testValue");
        assertEquals("testValue", map.get("testKey"));
    }
}
