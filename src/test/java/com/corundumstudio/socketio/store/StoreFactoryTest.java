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

import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    @BeforeEach
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
        
        assertNotNull(store, "Store should not be null");
        assertTrue(store instanceof Store, "Store should implement Store interface");
    }

    @Test
    public void testCreatePubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull(pubSubStore, "PubSubStore should not be null");
        assertTrue(pubSubStore instanceof PubSubStore, "PubSubStore should implement PubSubStore interface");
    }

    @Test
    public void testCreateMap() {
        String mapName = "testMap";
        Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull(map, "Map should not be null");
        assertTrue(map instanceof Map, "Map should implement Map interface");
    }

    @Test
    public void testCreateMultipleStores() {
        UUID sessionId1 = UUID.randomUUID();
        UUID sessionId2 = UUID.randomUUID();
        
        Store store1 = storeFactory.createStore(sessionId1);
        Store store2 = storeFactory.createStore(sessionId2);
        
        assertNotNull(store1, "First store should not be null");
        assertNotNull(store2, "Second store should not be null");
        assertNotSame(store1, store2, "Stores should be different instances");
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
        assertFalse(store2.has("isolatedKey"), "Store2 should not have data from store1");
        assertNull(store2.get("isolatedKey"), "Store2 should not return data from store1");
        
        // Store1 should still have the data
        assertTrue(store1.has("isolatedKey"), "Store1 should have its data");
        assertEquals(store1.get("isolatedKey"), "store1Value", "Store1 should return its data");
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
