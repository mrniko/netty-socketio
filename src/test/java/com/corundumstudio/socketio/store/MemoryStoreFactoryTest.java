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

import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MemoryStoreFactory - no container needed as it's in-memory
 */
public class MemoryStoreFactoryTest extends StoreFactoryTest {

    @Override
    protected StoreFactory createStoreFactory() throws Exception {
        return new MemoryStoreFactory();
    }

    @Test
    public void testMemorySpecificFeatures() {
        // Test that the factory creates Memory-specific stores
        UUID sessionId = UUID.randomUUID();
        Store store = storeFactory.createStore(sessionId);
        
        assertNotNull(store, "Store should not be null");
        assertTrue(store instanceof MemoryStore, "Store should be MemoryStore");
        
        // Test that the store works with memory storage
        store.set("memoryKey", "memoryValue");
        assertEquals("memoryValue", store.get("memoryKey"));
    }

    @Test
    public void testMemoryPubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull(pubSubStore, "PubSubStore should not be null");
        assertTrue(pubSubStore instanceof MemoryPubSubStore, "PubSubStore should be MemoryPubSubStore");
    }

    @Test
    public void testMemoryMapCreation() {
        String mapName = "testMemoryMap";
        java.util.Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull(map, "Map should not be null");
        assertTrue(map instanceof java.util.Map, "Map should implement Map interface");
        
        // Test that the map works
        map.put("testKey", "testValue");
        assertEquals("testValue", map.get("testKey"));
    }

    @Test
    public void testMemoryStoreIsolation() {
        // Test that different stores are isolated
        UUID sessionId1 = UUID.randomUUID();
        UUID sessionId2 = UUID.randomUUID();
        
        Store store1 = storeFactory.createStore(sessionId1);
        Store store2 = storeFactory.createStore(sessionId2);
        
        // Set data in store1
        store1.set("isolatedKey", "store1Value");
        
        // Store2 should not have this data
        assertFalse(store2.has("isolatedKey"), "Store2 should not have data from store1");
        assertNull(store2.get("isolatedKey"), "Store2 should not return data from store1");
        
        // Store1 should still have the data
        assertTrue(store1.has("isolatedKey"), "Store1 should have its data");
        assertEquals(store1.get("isolatedKey"), "store1Value", "Store1 should return its data");
    }
}
