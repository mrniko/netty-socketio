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
package com.corundumstudio.socketio.scheduler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class CancelableScheduler {

    protected final ConcurrentMap<SchedulerKey, Timeout> scheduledFutures = PlatformDependent.newConcurrentHashMap();
    protected final HashedWheelTimer executorService;
	
    protected volatile ChannelHandlerContext ctx;

    public CancelableScheduler() {
        executorService = new HashedWheelTimer();
    }
    
    public CancelableScheduler(ThreadFactory threadFactory) {
        executorService = new HashedWheelTimer(threadFactory);
    }    
    
    public void update(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
    
    public void cancel(SchedulerKey key) {
    	final Timeout timeout = scheduledFutures.remove(key);
        if (timeout != null) {
            timeout.cancel();
        }
    }
    
    public void schedule(final Runnable runnable, long delay, TimeUnit unit) {
        executorService.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                runnable.run();
            }
        }, delay, unit);
    }
    

    public abstract void schedule(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    public abstract void scheduleCallback(SchedulerKey key, Runnable runnable, long delay, TimeUnit unit);

    public void shutdown() {
        executorService.stop();
    }
    
}
