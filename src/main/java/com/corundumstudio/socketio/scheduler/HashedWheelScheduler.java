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
package com.corundumstudio.socketio.scheduler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HashedWheelScheduler implements CancelableScheduler {

    private final Map<SchedulerKey, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<SchedulerKey, ScheduledFuture<?>>();

    private volatile ChannelHandlerContext ctx;

    @Override
    public void update(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void cancel(SchedulerKey key) {
        ScheduledFuture<?> timeout = scheduledFutures.remove(key);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    public void schedule(final Runnable runnable, long delay, TimeUnit unit) {
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay, unit);
    }

    public void scheduleCallback(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        ScheduledFuture<?> timeout = ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    scheduledFutures.remove(key);
                }
            }
        }, delay, unit);

        if (!timeout.isDone()) {
            scheduledFutures.put(key, timeout);
        }
    }

    public void schedule(final SchedulerKey key, final Runnable runnable, long delay, TimeUnit unit) {
        ScheduledFuture<?> timeout = ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    scheduledFutures.remove(key);
                }
            }
        }, delay, unit);

        if (!timeout.isDone()) {
            scheduledFutures.put(key, timeout);
        }
    }

}
