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
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for RedissonStore using testcontainers
 */
public class RedissonStoreTest extends AbstractStoreTest {

    private RedissonClient redissonClient;

    @Override
    protected GenericContainer<?> createContainer() {
        return new CustomizedRedisContainer();
    }

    @Override
    protected Store createStore(UUID sessionId) throws Exception {
        CustomizedRedisContainer customizedRedisContainer = (CustomizedRedisContainer) container;
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + customizedRedisContainer.getHost() + ":" + customizedRedisContainer.getRedisPort());
        
        redissonClient = Redisson.create(config);
        return new RedissonStore(sessionId, redissonClient);
    }

    @Override
    protected void cleanupStore() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    public void testRedissonSpecificFeatures() {
        // Test that the store is actually using Redisson
        assertNotNull(store);
        
        // Test Redis-specific features like TTL (if supported)
        store.set("ttlKey", "ttlValue");
        assertEquals("ttlValue", store.get("ttlKey"));
    }

    @Test
    public void testRedisDataPersistence() {
        // Test that data persists across operations
        store.set("persistentKey", "persistentValue");
        assertEquals("persistentValue", store.get("persistentKey"));
        
        // Verify the key exists
        assertTrue(store.has("persistentKey"));
        
        // Delete and verify it's gone
        store.del("persistentKey");
        assertFalse(store.has("persistentKey"));
        assertNull(store.get("persistentKey"));
    }
}
