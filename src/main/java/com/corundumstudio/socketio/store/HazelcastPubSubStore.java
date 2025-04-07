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

import io.netty.util.internal.PlatformDependent;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;


public class HazelcastPubSubStore implements PubSubStore {

    private final HazelcastInstance hazelcastPub;
    private final HazelcastInstance hazelcastSub;
    private final Long nodeId;

    private final ConcurrentMap<String, Queue<UUID>> map = PlatformDependent.newConcurrentHashMap();

    public HazelcastPubSubStore(HazelcastInstance hazelcastPub, HazelcastInstance hazelcastSub, Long nodeId) {
        this.hazelcastPub = hazelcastPub;
        this.hazelcastSub = hazelcastSub;
        this.nodeId = nodeId;
    }

    @Override
    public void publish(PubSubType type, PubSubMessage msg) {
        msg.setNodeId(nodeId);
        hazelcastPub.getTopic(type.toString()).publish(msg);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, final PubSubListener<T> listener, Class<T> clazz) {
        String name = type.toString();
        ITopic<T> topic = hazelcastSub.getTopic(name);
        UUID regId = topic.addMessageListener(new MessageListener<T>() {
            @Override
            public void onMessage(Message<T> message) {
                PubSubMessage msg = message.getMessageObject();
                if (!nodeId.equals(msg.getNodeId())) {
                    listener.onMessage(message.getMessageObject());
                }
            }
        });

        Queue<UUID> list = map.get(name);
        if (list == null) {
            list = new ConcurrentLinkedQueue<UUID>();
            Queue<UUID> oldList = map.putIfAbsent(name, list);
            if (oldList != null) {
                list = oldList;
            }
        }
        list.add(regId);
    }

    @Override
    public void unsubscribe(PubSubType type) {
        String name = type.toString();
        Queue<UUID> regIds = map.remove(name);
        ITopic<Object> topic = hazelcastSub.getTopic(name);
        for (UUID id : regIds) {
            topic.removeMessageListener(id);
        }
    }

    @Override
    public void shutdown() {
    }

}
