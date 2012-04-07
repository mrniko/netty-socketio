/**
 * Copyright (c) 2012 Nikita Koksharov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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

	private final int heartbeatIntervalSecs;
	private final int heartbeatTimeoutSecs;
	
	private final ScheduledExecutorService executorService;
	private final Map<UUID, Future<?>> scheduledHeartbeatFutures = new ConcurrentHashMap<UUID, Future<?>>();
	
	public HeartbeatHandler(int threadPoolSize, int heartbeatTimeoutSecs, int heartbeatIntervalSecs) {
		this.executorService = Executors.newScheduledThreadPool(threadPoolSize);
		this.heartbeatIntervalSecs = heartbeatIntervalSecs;
		this.heartbeatTimeoutSecs = heartbeatTimeoutSecs;
	}
	
	public void onHeartbeat(final SocketIOClient client) {
		cancelHeartbeatCheck(client);
		
		executorService.schedule(new Runnable() {
			public void run() {
				sendHeartbeat(client);
			}
		}, heartbeatIntervalSecs, TimeUnit.SECONDS);
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
		Future<?> future = executorService.schedule(runnable, heartbeatTimeoutSecs, TimeUnit.SECONDS);
		scheduledHeartbeatFutures.put(sessionId, future);
	}

	public void shutdown() {
		executorService.shutdown();
	}
	
}
