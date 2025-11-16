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
import org.testcontainers.containers.GenericContainer;

import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for HazelcastStoreFactory using testcontainers
 */
public class HazelcastStoreFactoryTest extends StoreFactoryTest {

    private GenericContainer<?> container;
    private HazelcastInstance hazelcastInstance;
    private AutoCloseable closeableMocks;

    @Override
    protected StoreFactory createStoreFactory() throws Exception {
        container = new CustomizedHazelcastContainer();
        container.start();
        
        CustomizedHazelcastContainer customizedHazelcastContainer = (CustomizedHazelcastContainer) container;
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
        clientConfig.getNetworkConfig().addAddress(
            customizedHazelcastContainer.getHost() + ":" + customizedHazelcastContainer.getHazelcastPort()
        );
        
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
        return new HazelcastStoreFactory(hazelcastInstance);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeableMocks != null) {
            closeableMocks.close();
        }
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
        
        // Verify the Hazelcast map is destroyed
        // After destroy, the map should be empty or not accessible
        IMap<String, Object> map = hazelcastInstance.getMap(sessionId.toString());
        assertTrue(map.isEmpty() || map.size() == 0, "Map should be empty after destroy");
    }
}
