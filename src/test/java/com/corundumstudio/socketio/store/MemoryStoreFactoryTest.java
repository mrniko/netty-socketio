package com.corundumstudio.socketio.store;

import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

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
        
        assertNotNull("Store should not be null", store);
        assertTrue("Store should be MemoryStore", store instanceof MemoryStore);
        
        // Test that the store works with memory storage
        store.set("memoryKey", "memoryValue");
        assertEquals("memoryValue", store.get("memoryKey"));
    }

    @Test
    public void testMemoryPubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull("PubSubStore should not be null", pubSubStore);
        assertTrue("PubSubStore should be MemoryPubSubStore", pubSubStore instanceof MemoryPubSubStore);
    }

    @Test
    public void testMemoryMapCreation() {
        String mapName = "testMemoryMap";
        java.util.Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull("Map should not be null", map);
        assertTrue("Map should implement Map interface", map instanceof java.util.Map);
        
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
        assertFalse("Store2 should not have data from store1", store2.has("isolatedKey"));
        assertNull("Store2 should not return data from store1", store2.get("isolatedKey"));
        
        // Store1 should still have the data
        assertTrue("Store1 should have its data", store1.has("isolatedKey"));
        assertEquals("Store1 should return its data", "store1Value", store1.get("isolatedKey"));
    }
}
