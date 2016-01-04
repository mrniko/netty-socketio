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
import java.util.UUID;

import org.redisson.RedissonClient;

public class RedissonStore implements Store {

    private final Map<String, Object> map;

    public RedissonStore(Long sessionId, RedissonClient redisson) {
        this.map = redisson.getMap(sessionId.toString());
    }

    @Override
    public void set(String key, Object value) {
        map.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    @Override
    public boolean has(String key) {
        return map.containsKey(key);
    }

    @Override
    public void del(String key) {
        map.remove(key);
    }

}
