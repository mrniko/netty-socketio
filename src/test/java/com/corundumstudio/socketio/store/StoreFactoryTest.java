package com.corundumstudio.socketio.store;

import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for StoreFactory implementations
 */
public abstract class StoreFactoryTest {

    @Mock
    protected NamespacesHub namespacesHub;
    
    @Mock
    protected AuthorizeHandler authorizeHandler;
    
    @Mock
    protected JsonSupport jsonSupport;

    protected StoreFactory storeFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        storeFactory = createStoreFactory();
        storeFactory.init(namespacesHub, authorizeHandler, jsonSupport);
    }

    /**
     * Create the specific StoreFactory implementation to test
     */
    protected abstract StoreFactory createStoreFactory() throws Exception;

    @Test
    public void testCreateStore() {
        UUID sessionId = UUID.randomUUID();
        Store store = storeFactory.createStore(sessionId);
        
        assertNotNull("Store should not be null", store);
        assertTrue("Store should implement Store interface", store instanceof Store);
    }

    @Test
    public void testCreatePubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull("PubSubStore should not be null", pubSubStore);
        assertTrue("PubSubStore should implement PubSubStore interface", pubSubStore instanceof PubSubStore);
    }

    @Test
    public void testCreateMap() {
        String mapName = "testMap";
        Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull("Map should not be null", map);
        assertTrue("Map should implement Map interface", map instanceof Map);
    }

    @Test
    public void testCreateMultipleStores() {
        UUID sessionId1 = UUID.randomUUID();
        UUID sessionId2 = UUID.randomUUID();
        
        Store store1 = storeFactory.createStore(sessionId1);
        Store store2 = storeFactory.createStore(sessionId2);
        
        assertNotNull("First store should not be null", store1);
        assertNotNull("Second store should not be null", store2);
        assertNotSame("Stores should be different instances", store1, store2);
    }

    @Test
    public void testStoreIsolation() {
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

    @Test
    public void testShutdown() {
        // Create some stores first
        UUID sessionId = UUID.randomUUID();
        Store store = storeFactory.createStore(sessionId);
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        // Shutdown should not throw exception
        storeFactory.shutdown();
        
        // After shutdown, we might not be able to create new stores
        // This depends on the implementation
        try {
            Store newStore = storeFactory.createStore(UUID.randomUUID());
            // If we can create a store, that's fine
        } catch (Exception e) {
            // If we can't create a store after shutdown, that's also fine
        }
    }
}
