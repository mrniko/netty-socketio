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

import com.corundumstudio.socketio.store.pubsub.BaseStoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

/**
 * WARN: It's necessary to add netty-socketio.jar in hazelcast server classpath.
 *
 */
public class HazelcastStoreFactory extends BaseStoreFactory {

    private final HazelcastInstance hazelcastClient;
    private final HazelcastInstance hazelcastPub;
    private final HazelcastInstance hazelcastSub;

    private final PubSubStore pubSubStore;

    public HazelcastStoreFactory() {
        this(HazelcastClient.newHazelcastClient());
    }

    public HazelcastStoreFactory(HazelcastInstance instance) {
        this.hazelcastClient = instance;
        this.hazelcastPub = instance;
        this.hazelcastSub = instance;

        this.pubSubStore = new HazelcastPubSubStore(hazelcastPub, hazelcastSub, getNodeId());
    }

    public HazelcastStoreFactory(HazelcastInstance hazelcastClient, HazelcastInstance hazelcastPub, HazelcastInstance hazelcastSub) {
        this.hazelcastClient = hazelcastClient;
        this.hazelcastPub = hazelcastPub;
        this.hazelcastSub = hazelcastSub;

        this.pubSubStore = new HazelcastPubSubStore(hazelcastPub, hazelcastSub, getNodeId());
    }

    @Override
    public Store createStore(Long sessionId) {
        return new HazelcastStore(sessionId, hazelcastClient);
    }

    @Override
    public void shutdown() {
        hazelcastClient.shutdown();
        hazelcastPub.shutdown();
        hazelcastSub.shutdown();
    }

    @Override
    public PubSubStore pubSubStore() {
        return pubSubStore;
    }

    @Override
    public <K, V> Map<K, V> createMap(String name) {
        return hazelcastClient.getMap(name);
    }

}
