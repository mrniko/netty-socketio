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

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.redisson.Redisson;
import org.redisson.core.MessageListener;
import org.redisson.core.RTopic;

import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

public class RedissonPubSubStore implements PubSubStore {

    private final Redisson redissonPub;
    private final Redisson redissonSub;
    private final Long nodeId;

    private final ConcurrentMap<String, Queue<Integer>> map =
                                        new ConcurrentHashMap<String, Queue<Integer>>();

    public RedissonPubSubStore(Redisson redissonPub, Redisson redissonSub, Long nodeId) {
        this.redissonPub = redissonPub;
        this.redissonSub = redissonSub;
        this.nodeId = nodeId;
    }

    @Override
    public void publish(String name, PubSubMessage msg) {
        msg.setNodeId(nodeId);
        redissonPub.getTopic(name).publish(msg);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(String name, final PubSubListener<T> listener, Class<T> clazz) {
        RTopic<T> topic = redissonSub.getTopic(name);
        int regId = topic.addListener(new MessageListener<T>() {
            @Override
            public void onMessage(T msg) {
                if (!nodeId.equals(msg.getNodeId())) {
                    listener.onMessage(msg);
                }
            }
        });

        Queue<Integer> list = map.get(name);
        if (list == null) {
            list = new ConcurrentLinkedQueue<Integer>();
            Queue<Integer> oldList = map.putIfAbsent(name, list);
            if (oldList != null) {
                list = oldList;
            }
        }
        list.add(regId);
    }

    @Override
    public void unsubscribe(String name) {
        Queue<Integer> regIds = map.remove(name);
        RTopic<Object> topic = redissonSub.getTopic(name);
        for (Integer id : regIds) {
            topic.removeListener(id);
        }
    }

    @Override
    public void shutdown() {
    }

}
