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


/**
 * Store interface for managing session-specific data storage.
 * <p>
 * Each store instance is associated with a specific session and provides
 * key-value storage operations. The store can be backed by different
 * storage implementations (in-memory, Hazelcast, Redisson, etc.).
 * </p>
 */
public interface Store {

    /**
     * Sets a value for the specified key in this store.
     *
     * @param key the key to set
     * @param val the value to store (must not be null)
     * @throws NullPointerException if the value is null
     */
    void set(String key, Object val);

    /**
     * Gets the value associated with the specified key.
     *
     * @param <T> the type of the value to retrieve
     * @param key the key to retrieve
     * @return the value associated with the key, or null if the key does not exist
     */
    <T> T get(String key);

    /**
     * Checks whether a key exists in this store.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    boolean has(String key);

    /**
     * Deletes the value associated with the specified key.
     *
     * @param key the key to delete
     */
    void del(String key);

    /**
     * Destroys or clears all data in this store.
     * <p>
     * This method should be called when the store is no longer needed,
     * typically when a client disconnects. After calling this method,
     * the store should be considered invalid and should not be used further.
     * </p>
     * <p>
     * The exact behavior depends on the implementation:
     * <ul>
     *   <li>For distributed stores (Hazelcast, Redisson), this will delete
     *       the entire map/collection associated with the session.</li>
     *   <li>For in-memory stores, this will clear all stored data.</li>
     * </ul>
     * </p>
     */
    void destroy();

}
