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
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.corundumstudio.socketio.handler.SocketIOException;
import com.corundumstudio.socketio.parser.JsonSupport;

public class RedisStore implements PubSubStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Jedis client;
    private Jedis sub;
    private Jedis pub;

    private final String sessionId;
    private final Long nodeId;
    private final JsonSupport jsonSupport;

    private final ConcurrentMap<String, Queue<JedisPubSub>> map =
                                                new ConcurrentHashMap<String, Queue<JedisPubSub>>();

    public RedisStore(UUID sessionId, Long nodeId, JsonSupport jsonSupport) {
        this.sessionId = sessionId.toString();
        this.nodeId = nodeId;
        this.jsonSupport = jsonSupport;
    }

    public void setClient(Jedis client) {
        this.client = client;
    }

    public void setPub(Jedis pub) {
        this.pub = pub;
    }

    public void setSub(Jedis sub) {
        this.sub = sub;
    }

    @Override
    public void set(String key, String value) {
        client.hset(sessionId, key, value);
    }

    @Override
    public String get(String key) {
        return client.hget(sessionId, key);
    }

    @Override
    public boolean has(String key) {
        return client.hexists(sessionId, key);
    }

    @Override
    public void del(String key) {
        client.hdel(sessionId, key);
    }

    @Override
    public void publish(String name, Object... args) {
        RedisPublishMsg msg = new RedisPublishMsg(nodeId, Arrays.asList(args));
        try {
            pub.publish(name, jsonSupport.writeValueAsString(msg));
        } catch (IOException e) {
            throw new SocketIOException(e);
        }
    }

    @Override
    public void subscribe(final String name, final MessageListener listener) {
        JedisPubSub jedisPubSub = new JedisPubSub() {

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
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
                    RedisPublishMsg msg = jsonSupport.readValue(message, RedisPublishMsg.class);
                    if (!nodeId.equals(msg.getNodeId())) {
                        listener.onMessage(msg.getArgs().toArray());
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }

            }
        };

        Queue<JedisPubSub> list = map.get(name);
        if (list == null) {
            list = new ConcurrentLinkedQueue<JedisPubSub>();
            Queue<JedisPubSub> oldList = map.putIfAbsent(name, list);
            if (oldList != null) {
                list = oldList;
            }
        }
        list.add(jedisPubSub);

        sub.subscribe(jedisPubSub, name);
    }

    @Override
    public void unsubscribe(String name) {
        Queue<JedisPubSub> list = map.remove(name);
        if (list != null) {
            for (JedisPubSub jedisPubSub : list) {
                jedisPubSub.unsubscribe();
            }
        }
    }

}
