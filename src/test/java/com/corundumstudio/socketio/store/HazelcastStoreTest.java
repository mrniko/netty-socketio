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

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for HazelcastStore using testcontainers
 */
public class HazelcastStoreTest extends AbstractStoreTest {

    private HazelcastInstance hazelcastInstance;

    @Override
    protected GenericContainer<?> createContainer() {
        return new CustomizedHazelcastContainer();
    }

    @Override
    protected Store createStore(UUID sessionId) throws Exception {
        CustomizedHazelcastContainer customizedHazelcastContainer = (CustomizedHazelcastContainer) container;
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
        clientConfig.getNetworkConfig().addAddress(
            customizedHazelcastContainer.getHost() + ":" + customizedHazelcastContainer.getHazelcastPort()
        );
        
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
        return new HazelcastStore(sessionId, hazelcastInstance);
    }

    @Override
    protected void cleanupStore() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
        }
    }

    @Test
    public void testHazelcastSpecificFeatures() {
        // Test that the store is actually using Hazelcast
        assertNotNull(store);
        
        // Test large object storage
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        store.set("largeData", largeData);
        byte[] retrieved = store.get("largeData");
        assertNotNull(retrieved);
        assertEquals(largeData.length, retrieved.length);
    }
}
