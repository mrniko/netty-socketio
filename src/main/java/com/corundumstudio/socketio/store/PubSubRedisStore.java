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

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.corundumstudio.socketio.handler.SocketIOException;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

public class PubSubRedisStore implements PubSubStore {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Jedis sub;
    private final Jedis pub;

    private final Long nodeId;
    private final JsonSupport jsonSupport;

    private final ConcurrentMap<String, Class> mapping = new ConcurrentHashMap<String, Class>();

    private final ConcurrentMap<String, Queue<PubSubListener>> map =
                                                new ConcurrentHashMap<String, Queue<PubSubListener>>();

    // used to sync subscription process
    final Semaphore s = new Semaphore(1);

    final JedisPubSub jedisPubSub = new JedisPubSub() {

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            s.release();
        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {
        }

        @Override
        public void onMessage(String channel, String message) {
            try {
                log.trace("onMessage: {}", message);

                Class<PubSubMessage> clazz = mapping.get(channel);
                // could be deleted
                if (clazz == null) {
                    return;
                }
                PubSubMessage data = jsonSupport.readValue(message, clazz);
                if (!nodeId.equals(data.getNodeId())) {
                    Queue<PubSubListener> listeners = map.get(channel);
                    // could be deleted
                    if (listeners == null) {
                        return;
                    }
                    for (PubSubListener listener : listeners) {
                        listener.onMessage(data);
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

        }
    };

    public PubSubRedisStore(Jedis pub, Jedis sub, Long nodeId, JsonSupport jsonSupport) {
        this.nodeId = nodeId;
        this.jsonSupport = jsonSupport;
        this.pub = pub;
        this.sub = sub;
    }

    @Override
    public void publish(String name, PubSubMessage msg) {
        msg.setNodeId(nodeId);
        log.trace("publish: {} msg: {}", name, msg);
        try {
            pub.publish(name, jsonSupport.writeValueAsString(msg));
        } catch (IOException e) {
            throw new SocketIOException(e);
        }
    }

    @Override
    public <T> void subscribe(final String name, PubSubListener<T> listener, Class<T> clazz) {
        Queue<PubSubListener> list = map.get(name);
        if (list == null) {
            list = new ConcurrentLinkedQueue<PubSubListener>();
            Queue<PubSubListener> oldList = map.putIfAbsent(name, list);
            if (oldList != null) {
                list = oldList;
            }
        }
        list.add(listener);

        mapping.put(name, clazz);

        s.acquireUninterruptibly();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sub.subscribe(jedisPubSub, name);
                } catch (Exception e) {
                    log.error("error", e);
                }
            }
        });

        s.acquireUninterruptibly();
        s.release();
    }

    @Override
    public void unsubscribe(String name) {
        mapping.remove(name);
        map.remove(name);
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
