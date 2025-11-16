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
package com.corundumstudio.socketio.store;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for RedissonStoreFactory using testcontainers
 */
public class RedissonStoreFactoryTest extends StoreFactoryTest {

    private GenericContainer<?> container;
    private RedissonClient redissonClient;
    private AutoCloseable closeableMocks;

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
        if (closeableMocks != null) {
            closeableMocks.close();
        }
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
        Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull(map, "Map should not be null");
        assertTrue(map instanceof Map, "Map should implement Map interface");
        
        // Test that the map works
        map.put("testKey", "testValue");
        assertEquals("testValue", map.get("testKey"));
    }

    @Test
    public void testOnDisconnect() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        
        UUID sessionId = UUID.randomUUID();
        Store store = storeFactory.createStore(sessionId);
        
        // Add some data to the store
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("key3", 123);
        
        // Verify data exists
        assertTrue(store.has("key1"));
        assertEquals("value1", store.get("key1"));
        assertTrue(store.has("key2"));
        assertEquals("value2", store.get("key2"));
        assertTrue(store.has("key3"));
        assertEquals(Integer.valueOf(123), store.get("key3"));
        
        // Create a mock ClientHead
        ClientHead clientHead = Mockito.mock(ClientHead.class);
        when(clientHead.getSessionId()).thenReturn(sessionId);
        when(clientHead.getStore()).thenReturn(store);
        
        // Call onDisconnect
        storeFactory.onDisconnect(clientHead);
        
        // Verify the Redisson map is deleted
        // After delete, the map should be empty or not accessible
        RMap<String, Object> map = redissonClient.getMap(sessionId.toString());
        assertTrue(map.isEmpty() || map.size() == 0, "Map should be empty after delete");
    }
}
