package com.corundumstudio.socketio.store;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.corundumstudio.socketio.store.CustomizedHazelcastContainer;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for HazelcastStoreFactory using testcontainers
 */
public class HazelcastStoreFactoryTest extends StoreFactoryTest {

    private GenericContainer<?> container;
    private HazelcastInstance hazelcastInstance;

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

    @After
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
        
        assertNotNull("Store should not be null", store);
        assertTrue("Store should be HazelcastStore", store instanceof HazelcastStore);
        
        // Test that the store works with Hazelcast
        store.set("hazelcastKey", "hazelcastValue");
        assertEquals("hazelcastValue", store.get("hazelcastKey"));
    }

    @Test
    public void testHazelcastPubSubStore() {
        PubSubStore pubSubStore = storeFactory.pubSubStore();
        
        assertNotNull("PubSubStore should not be null", pubSubStore);
        assertTrue("PubSubStore should be HazelcastPubSubStore", pubSubStore instanceof HazelcastPubSubStore);
    }

    @Test
    public void testHazelcastMapCreation() {
        String mapName = "testHazelcastMap";
        java.util.Map<String, Object> map = storeFactory.createMap(mapName);
        
        assertNotNull("Map should not be null", map);
        assertTrue("Map should implement Map interface", map instanceof java.util.Map);
        
        // Test that the map works
        map.put("testKey", "testValue");
        assertEquals("testValue", map.get("testKey"));
    }
}
