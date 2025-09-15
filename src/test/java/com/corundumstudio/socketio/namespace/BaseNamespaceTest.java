/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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
package com.corundumstudio.socketio.namespace;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base test class for Namespace tests providing shared thread pool and utility methods.
 */
public abstract class BaseNamespaceTest {

    protected static ExecutorService sharedExecutor;
    protected static final int DEFAULT_TASK_COUNT = 10;
    protected static final int DEFAULT_TIMEOUT_SECONDS = 5;

    @BeforeAll
    static void setUpSharedResources() {
        sharedExecutor = Executors.newFixedThreadPool(DEFAULT_TASK_COUNT);
    }

    @AfterAll
    static void tearDownSharedResources() throws InterruptedException {
        if (sharedExecutor != null) {
            sharedExecutor.shutdown();
            if (!sharedExecutor.awaitTermination(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                sharedExecutor.shutdownNow();
            }
        }
    }

    /**
     * Execute concurrent operations using the shared thread pool.
     *
     * @param taskCount number of tasks to execute concurrently
     * @param operation the operation to execute in each task
     * @return the countdown latch for synchronization
     */
    protected CountDownLatch executeConcurrentOperations(int taskCount, Runnable operation) {
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            sharedExecutor.submit(
                    () -> {
                        try {
                            operation.run();
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        return latch;
    }

    /**
     * Execute concurrent operations with index using the shared thread pool.
     *
     * @param taskCount number of tasks to execute concurrently
     * @param operation the operation to execute in each task (receives task index)
     * @return the countdown latch for synchronization
     */
    protected CountDownLatch executeConcurrentOperationsWithIndex(
            int taskCount, IndexedOperation operation) {
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            final int index = i;
            sharedExecutor.submit(
                    () -> {
                        try {
                            operation.run(index);
                        } finally {
                            latch.countDown();
                        }
                    });
        }

        return latch;
    }

    /**
     * Wait for concurrent operations to complete with timeout.
     *
     * @param latch the countdown latch
     * @param timeoutSeconds timeout in seconds
     * @throws InterruptedException if interrupted
     */
    protected void waitForCompletion(CountDownLatch latch, int timeoutSeconds)
            throws InterruptedException {
        latch.await(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Wait for concurrent operations to complete with default timeout.
     *
     * @param latch the countdown latch
     * @throws InterruptedException if interrupted
     */
    protected void waitForCompletion(CountDownLatch latch) throws InterruptedException {
        waitForCompletion(latch, DEFAULT_TIMEOUT_SECONDS);
    }

    /** Functional interface for operations that need task index. */
    @FunctionalInterface
    protected interface IndexedOperation {
        void run(int index);
    }
}
