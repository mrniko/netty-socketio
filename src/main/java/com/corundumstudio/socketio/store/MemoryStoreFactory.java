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

import com.corundumstudio.socketio.Store;
import com.corundumstudio.socketio.store.pubsub.BaseStoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

public class MemoryStoreFactory extends BaseStoreFactory {

    private final PubSubMemoryStore pubSubMemoryStore = new PubSubMemoryStore();

    @Override
    public Store create(UUID sessionId) {
        return new MemoryStore();
    }

    @Override
    public PubSubStore getPubSubStore() {
        return pubSubMemoryStore;
    }

    @Override
    public void shutdown() {
    }

}
