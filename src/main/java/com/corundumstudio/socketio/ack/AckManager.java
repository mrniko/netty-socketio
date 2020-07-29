/**
 * Copyright (c) 2012-2019 Nikita Koksharov
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
package com.corundumstudio.socketio.ack;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AckManager implements Disconnectable {

    class AckEntry {

        final Map<Long, AckCallback<?>> ackCallbacks = new ConcurrentSkipListMap<Long, AckCallback<?>>();
        final AtomicLong ackIndex = new AtomicLong(-1);

        public long addAckCallback(AckCallback<?> callback) {
            long index = ackIndex.incrementAndGet();
            ackCallbacks.put(index, callback);
            return index;
        }

        public Set<Long> getAckIndexes() {
            return ackCallbacks.keySet();
        }

        public AckCallback<?> getAckCallback(long index) {
            return ackCallbacks.get(index);
        }

        public AckCallback<?> removeCallback(long index) {
            return ackCallbacks.remove(index);
        }

        public void initAckIndex(long index) {
            ackIndex.compareAndSet(-1, index);
        }

    }

    private static final Logger log = LoggerFactory.getLogger(AckManager.class);

    private final ConcurrentMap<UUID, AckEntry> ackEntries = new ConcurrentSkipListMap<UUID, AckEntry>();

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
            AckEntry oldAckEntry = ackEntries.putIfAbsent(sessionId, ackEntry);
            if (oldAckEntry != null) {
                ackEntry = oldAckEntry;
            }
        }
        return ackEntry;
    }

    @SuppressWarnings("unchecked")
    public void onAck(SocketIOClient client, Packet packet) {
        AckSchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, client.getSessionId(), packet.getAckId());
        scheduler.cancel(key);

        AckCallback callback = removeCallback(client.getSessionId(), packet.getAckId());
        if (callback == null) {
            return;
        }
        if (callback instanceof MultiTypeAckCallback) {
            callback.onSuccess(new MultiTypeArgs(packet.<List<Object>>getData()));
        } else {
            Object param = null;
            List<Object> args = packet.getData();
            if (!args.isEmpty()) {
                param = args.get(0);
            }
            if (args.size() > 1) {
                log.error("Wrong ack args amount. Should be only one argument, but current amount is: {}. Ack id: {}, sessionId: {}",
                        args.size(), packet.getAckId(), client.getSessionId());
            }
            callback.onSuccess(param);
        }
    }

    private AckCallback<?> removeCallback(UUID sessionId, long index) {
        AckEntry ackEntry = ackEntries.get(sessionId);
        // may be null if client disconnected
        // before timeout occurs
        if (ackEntry != null) {
            return ackEntry.removeCallback(index);
        }
        return null;
    }

    public AckCallback<?> getCallback(UUID sessionId, long index) {
        AckEntry ackEntry = getAckEntry(sessionId);
        return ackEntry.getAckCallback(index);
    }

    public long registerAck(UUID sessionId, AckCallback<?> callback) {
        AckEntry ackEntry = getAckEntry(sessionId);
        ackEntry.initAckIndex(0);
        long index = ackEntry.addAckCallback(callback);

        if (log.isDebugEnabled()) {
            log.debug("AckCallback registered with id: {} for client: {}", index, sessionId);
        }

        scheduleTimeout(index, sessionId, callback);

        return index;
    }

    private void scheduleTimeout(final long index, final UUID sessionId, AckCallback<?> callback) {
        if (callback.getTimeout() == -1) {
            return;
        }
        SchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, sessionId, index);
        scheduler.scheduleCallback(key, new Runnable() {
            @Override
            public void run() {
                AckCallback<?> cb = removeCallback(sessionId, index);
                if (cb != null) {
                    cb.onTimeout();
                }
            }
        }, callback.getTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void onDisconnect(ClientHead client) {
        AckEntry e = ackEntries.remove(client.getSessionId());
        if (e == null) {
            return;
        }

        Set<Long> indexes = e.getAckIndexes();
        for (Long index : indexes) {
            AckCallback<?> callback = e.getAckCallback(index);
            if (callback != null) {
                callback.onTimeout();
            }
            SchedulerKey key = new AckSchedulerKey(Type.ACK_TIMEOUT, client.getSessionId(), index);
            scheduler.cancel(key);
        }
    }

}
