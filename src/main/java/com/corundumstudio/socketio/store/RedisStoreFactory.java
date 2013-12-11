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

import com.corundumstudio.socketio.Store;
import com.corundumstudio.socketio.StoreFactory;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.transport.MainBaseClient;

public class RedisStoreFactory implements StoreFactory {

    private final Jedis redisClient;
    private final Jedis redisPub;
    private final Jedis redisSub;
    private final Long nodeId = (long) (Math.random() * 1000000);

    private JsonSupport jsonSupport;

    public RedisStoreFactory(Jedis redisClient, Jedis redisPub, Jedis redisSub) {
        this.redisClient = redisClient;
        this.redisPub = redisPub;
        this.redisSub = redisSub;
    }

    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    @Override
    public Store create(UUID sessionId) {
        RedisStore store = new RedisStore(sessionId, nodeId, jsonSupport);
        store.setClient(redisClient);
        store.setPub(redisPub);
        store.setSub(redisSub);
        return store;
    }

    @Override
    public void onDisconnect(MainBaseClient client) {
        redisClient.del(client.getSessionId().toString());
    }

}
