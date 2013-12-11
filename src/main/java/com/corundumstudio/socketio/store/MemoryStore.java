/**
 * Copyright 2012 Nikita Koksharov
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
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore implements PubSubStore {

    private final Map<String, String> store = new ConcurrentHashMap<String, String>();

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public boolean has(String key) {
        return store.containsKey(key);
    }

    @Override
    public void del(String key) {
        store.remove(key);
    }

    @Override
    public void publish(String name, Object... args) {
    }

    @Override
    public void unsubscribe(String name) {
    }

    @Override
    public void subscribe(String name, MessageListener listener) {
    }

}
