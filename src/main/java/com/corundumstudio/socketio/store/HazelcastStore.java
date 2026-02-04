/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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

import com.hazelcast.core.HazelcastInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Hazelcast Store implementation compatible with Hazelcast 3.x, 4.x, and 5.x.
 */
public class HazelcastStore implements Store {

    private final MapOperations map;

    public HazelcastStore(UUID sessionId, HazelcastInstance hazelcastInstance) {
        try {
            Method getMapMethod = HazelcastInstance.class.getMethod("getMap", String.class);
            this.map = MapOperations.wrap(getMapMethod.invoke(hazelcastInstance, sessionId.toString()));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to get map: " + sessionId, e);
        }
    }

    @Override
    public void set(String key, Object val) {
        map.put(key, val);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) map.get(key);
    }

    @Override
    public boolean has(String key) {
        return map.containsKey(key);
    }

    @Override
    public void del(String key) {
        map.delete(key);
    }

    /**
     * Version-independent interface for Hazelcast IMap operations.
     */
    interface MapOperations {

        Object put(Object key, Object value);
        Object get(Object key);
        boolean containsKey(Object key);
        void delete(Object key);

        static MapOperations wrap(Object map) {
            return (MapOperations) Proxy.newProxyInstance(
                    MapOperations.class.getClassLoader(),
                    new Class<?>[]{MapOperations.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            try {
                                Method targetMethod = map.getClass().getMethod(method.getName(), method.getParameterTypes());
                                return targetMethod.invoke(map, args);
                            } catch (InvocationTargetException e) {
                                Throwable cause = e.getCause();
                                throw cause != null ? cause : e;
                            } catch (NoSuchMethodException | IllegalAccessException e) {
                                throw new IllegalStateException("Failed to invoke " + method.getName(), e);
                            }
                        }
                    }
            );
        }
    }
}