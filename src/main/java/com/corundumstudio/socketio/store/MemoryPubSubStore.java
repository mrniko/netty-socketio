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
import java.util.concurrent.Executors;

import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.store.pubsub.PubSubType;

import com.google.common.collect.Maps;
import com.google.common.eventbus.AsyncEventBus;

public class MemoryPubSubStore implements PubSubStore {
	
	private final AsyncEventBus asyncEventBus = new AsyncEventBus(Executors.newFixedThreadPool(3));
    
    private final Map<PubSubType,PubSubListener<?>> subtypeListener = Maps.newHashMap();
    
    private final Long nodeId;
    
    public MemoryPubSubStore(Long nodeId){
        this.nodeId = nodeId;
    }
	
    @Override
    public void publish(PubSubType type, PubSubMessage msg) {
		msg.setNodeId(nodeId);
        asyncEventBus.post(msg);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
		subtypeListener.put(type, listener);
        asyncEventBus.register(listener);
    }

    @Override
    public void unsubscribe(PubSubType type) {
		PubSubListener<?> ls = subtypeListener.get(type);
        asyncEventBus.unregister(ls);
        subtypeListener.remove(type);
    }

    @Override
    public void shutdown() {
    }

}
