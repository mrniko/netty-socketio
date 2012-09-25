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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;
import com.corundumstudio.socketio.transport.BaseClient;

public class AckManager implements Disconnectable {

    private class AckEntry {

        final Map<Long, AckCallback> ackCallbacks = new ConcurrentHashMap<Long, AckCallback>();
        final AtomicLong ackIndex = new AtomicLong(-1);

        public long addAckCallback(AckCallback callback) {
            long index = ackIndex.incrementAndGet();
            ackCallbacks.put(index, callback);
            return index;
        }

        public AckCallback getAckCallback(long index) {
            return ackCallbacks.get(index);
        }

        public AckCallback removeCallback(long index) {
            return ackCallbacks.remove(index);
        }

        public AtomicLong getAckIndex() {
            return ackIndex;
        }

        public void initAckIndex(long index) {
            ackIndex.compareAndSet(-1, index);
        }

    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, AckEntry> ackEntries = new ConcurrentHashMap<UUID, AckEntry>();

    private final CancelableScheduler scheduler;

    public AckManager(CancelableScheduler scheduler) {
        super();
        this.scheduler = scheduler;
    }

    public void initAckIndex(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(index);
    }

    private AckEntry getAckEntry(UUID sessionId) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        if (ackEntry == null) {
            ackEntry = new AckEntry();
            AckEntry oldAckEntry = ackEntries.put(sessionId, ackEntry);
            if (oldAckEntry != null) {
                ackEntry = oldAckEntry;
            }
        }
        return ackEntry;
    }

    public void onAck(SocketIOClient client, Packet packet) {
        SchedulerKey key = new SchedulerKey(Type.ACK_TIMEOUT, client.getSessionId());
        scheduler.cancel(key);

        AckCallback callback = removeCallback(client.getSessionId(), packet.getAckId());
        if (callback != null) {
            Object param = null;
            if (!packet.getArgs().isEmpty()) {
                param = packet.getArgs().get(0);
            }
            callback.onSuccess(param);
        }
    }

    private AckCallback removeCallback(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        return ackEntry.removeCallback(index);
    }

    public AckCallback<?> getCallback(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        return ackEntry.getAckCallback(index);
    }

    public long registerAck(UUID sessionId, AckCallback callback) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(0);
        long index = ackEntry.addAckCallback(callback);

        if (log.isDebugEnabled()) {
            log.debug("AckCallback registered with id: {}", index);
        }

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
    public void onDisconnect(BaseClient client) {
        ackEntries.remove(client.getSessionId());
    }

}
