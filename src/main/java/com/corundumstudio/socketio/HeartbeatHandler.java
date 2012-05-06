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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class HeartbeatHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, Future<?>> scheduledHeartbeatFutures = new ConcurrentHashMap<UUID, Future<?>>();
    private final ScheduledExecutorService executorService;
    private final Configuration configuration;

    public HeartbeatHandler(Configuration configuration) {
        this.executorService = Executors.newScheduledThreadPool(configuration.getHeartbeatThreadPoolSize());
        this.configuration = configuration;
    }

    public void onHeartbeat(final SocketIOClient client) {
        if (configuration.getHeartbeatTimeout() == 0) {
            return;
        }

        cancelHeartbeatCheck(client);

        executorService.schedule(new Runnable() {
            public void run() {
                sendHeartbeat(client);
            }
        }, configuration.getHeartbeatInterval(), TimeUnit.SECONDS);
    }

    public void cancelHeartbeatCheck(SocketIOClient client) {
        Future<?> future = scheduledHeartbeatFutures.remove(client.getSessionId());
        if (future != null) {
            future.cancel(false);
        }
    }

    public void sendHeartbeat(final SocketIOClient client) {
        client.send(new Packet(PacketType.HEARTBEAT));
        scheduleHeartbeatCheck(client.getSessionId(), new Runnable() {
            public void run() {
                try {
                    client.disconnect();
                } finally {
                    UUID sessionId = client.getSessionId();
                    scheduledHeartbeatFutures.remove(sessionId);
                    log.debug("Client with sessionId: {} disconnected due to heartbeat timeout", sessionId);
                }
            }
        });
    }

    public void scheduleHeartbeatCheck(UUID sessionId, Runnable runnable) {
        if (configuration.getHeartbeatTimeout() == 0) {
            return;
        }

        Future<?> future = executorService.schedule(runnable, configuration.getHeartbeatTimeout(), TimeUnit.SECONDS);
        scheduledHeartbeatFutures.put(sessionId, future);
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
