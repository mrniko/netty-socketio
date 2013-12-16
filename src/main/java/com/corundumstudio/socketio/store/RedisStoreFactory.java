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

import java.util.UUID;

import redis.clients.jedis.Jedis;

import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.store.pubsub.BaseStoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.transport.MainBaseClient;

public class RedisStoreFactory extends BaseStoreFactory {

    private final Long nodeId = (long) (Math.random() * 1000000);

    private Jedis redisClient = new Jedis("127.0.0.1", 6379);
    private Jedis redisPub = new Jedis("127.0.0.1", 6379);
    private Jedis redisSub = new Jedis("127.0.0.1", 6379);

    private PubSubRedisStore pubSubRedisStore;

    public RedisStoreFactory() {
    }

    public RedisStoreFactory(Jedis redisClient, Jedis redisPub, Jedis redisSub) {
        this.redisClient = redisClient;
        this.redisPub = redisPub;
        this.redisSub = redisSub;
    }

    @Override
    public void init(NamespacesHub namespacesHub, JsonSupport jsonSupport) {
        pubSubRedisStore = new PubSubRedisStore(redisPub, redisSub, nodeId, jsonSupport);

        redisClient.connect();
        redisPub.connect();
        redisSub.connect();

        super.init(namespacesHub, jsonSupport);
    }


    @Override
    public Store create(UUID sessionId) {
        return new RedisStore(sessionId, redisClient);
    }

    public PubSubStore getPubSubStore() {
        return pubSubRedisStore;
    }

    @Override
    public void onDisconnect(MainBaseClient client) {
        redisClient.del(client.getSessionId().toString());
    }

    @Override
    public void shutdown() {
        pubSubRedisStore.shutdown();

        redisClient.disconnect();
        redisPub.disconnect();
        redisSub.disconnect();
    }

}
