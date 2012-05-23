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
package com.corundumstudio.socketio;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;

public class AckManager implements Disconnectable {

    private final AtomicLong ackIndex = new AtomicLong();
    private final Map<Long, AckCallback> ackCallbacks = new ConcurrentHashMap<Long, AckCallback>();
    private final ConcurrentMap<UUID, Set<Long>> clientCallbackIds = new ConcurrentHashMap<UUID, Set<Long>>();

    private final CancelableScheduler scheduler;

    public AckManager(CancelableScheduler scheduler) {
        super();
        this.scheduler = scheduler;
    }

    public void onAck(SocketIOClient client, Packet packet) {
        SchedulerKey key = new SchedulerKey(Type.ACK_TIMEOUT, client.getSessionId());
        scheduler.cancel(key);
        AckCallback callback = removeCallback(client.getSessionId(), packet.getAckId());
        if (callback != null) {
            callback.onSuccess();
        }
    }

    private AckCallback removeCallback(UUID sessionId, long index) {
        AckCallback callback = ackCallbacks.remove(index);
        if (callback != null) {
            Set<Long> callbackIds = clientCallbackIds.get(sessionId);
            if (callbackIds != null) {
                callbackIds.remove(index);
            }
        }
        return callback;
    }

    public long registerAck(UUID sessionId, final AckCallback callback) {
        Set<Long> callbackIds = clientCallbackIds.get(sessionId);
        if (callbackIds == null) {
            callbackIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
            Set<Long> oldCallbackIds = clientCallbackIds.putIfAbsent(sessionId, callbackIds);
            if (oldCallbackIds != null) {
                callbackIds = oldCallbackIds;
            }
        }
        long index = ackIndex.incrementAndGet();
        callbackIds.add(index);
        ackCallbacks.put(index, callback);

        scheduleTimeout(index, sessionId, callback);

        return index;
    }

    private void scheduleTimeout(final long index, final UUID sessionId, final AckCallback callback) {
        if (callback.getTimeout() == -1) {
            return;
        }
        SchedulerKey key = new SchedulerKey(Type.ACK_TIMEOUT, sessionId);
        scheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                removeCallback(sessionId, index);
                callback.onTimeout();
            }
        }, callback.getTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        Set<Long> callbackIds = clientCallbackIds.remove(client.getSessionId());
        if (callbackIds != null) {
            ackCallbacks.keySet().removeAll(callbackIds);
        }
    }

}
