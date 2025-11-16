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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
        Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull(map, "Map should not be null");
        
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

    @Test
    public void testOnDisconnect() {
        AutoCloseable closeableMocks = MockitoAnnotations.openMocks(this);
        try {
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
            
            // Verify the MemoryStore is cleared
            assertFalse(store.has("key1"), "Store should not have key1 after clear");
            assertNull(store.get("key1"), "Store should return null for key1 after clear");
            assertFalse(store.has("key2"), "Store should not have key2 after clear");
            assertNull(store.get("key2"), "Store should return null for key2 after clear");
            assertFalse(store.has("key3"), "Store should not have key3 after clear");
            assertNull(store.get("key3"), "Store should return null for key3 after clear");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                closeableMocks.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
