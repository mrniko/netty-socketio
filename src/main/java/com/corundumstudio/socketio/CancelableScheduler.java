package com.corundumstudio.socketio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CancelableScheduler<T> {

    private final Map<T, Future<?>> scheduledFutures = new ConcurrentHashMap<T, Future<?>>();
    private final ScheduledExecutorService executorService;

    public CancelableScheduler(int threadPoolSize) {
    	executorService = Executors.newScheduledThreadPool(threadPoolSize);
	}

    public void cancel(T key) {
        Future<?> future = scheduledFutures.remove(key);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void schedule(Runnable runnable, long delay, TimeUnit unit) {
    	executorService.schedule(runnable, delay, unit);
    }

    public void schedule(final T key, final Runnable runnable, long delay, TimeUnit unit) {
        Future<?> future = executorService.schedule(new Runnable() {
			@Override
			public void run() {
                try {
    				runnable.run();
                } finally {
                	scheduledFutures.remove(key);
                }
			}
		}, delay, unit);
        scheduledFutures.put(key, future);
    }

    public void shutdown() {
    	executorService.shutdownNow();
    }

}
