/**
 * Copyright (c) 2012-2025 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.EventExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@DisplayName("HashedWheelScheduler Tests")
class HashedWheelSchedulerTest {

    private AutoCloseable autoCloseableMocks;

    @Mock
    private ChannelHandlerContext mockCtx;

    @Mock
    private EventExecutor mockExecutor;

    @Mock
    private EventLoop mockEventLoop;

    private HashedWheelScheduler scheduler;

    @BeforeEach
    void setUp() {
        autoCloseableMocks = MockitoAnnotations.openMocks(this);
        doReturn(mockExecutor).when(mockCtx).executor();
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(mockExecutor).execute(any(Runnable.class));

        scheduler = new HashedWheelScheduler();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        autoCloseableMocks.close();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create scheduler with default constructor")
        void shouldCreateSchedulerWithDefaultConstructor() {
            // When
            HashedWheelScheduler newScheduler = new HashedWheelScheduler();

            // Then
            assertThat(newScheduler).isNotNull();

            // Cleanup
            newScheduler.shutdown();
        }

        @Test
        @DisplayName("Should create scheduler with custom thread factory")
        void shouldCreateSchedulerWithCustomThreadFactory() {
            // Given
            java.util.concurrent.ThreadFactory customThreadFactory = r -> {
                Thread thread = new Thread(r);
                thread.setName("custom-scheduler-thread");
                return thread;
            };

            // When
            HashedWheelScheduler newScheduler = new HashedWheelScheduler(customThreadFactory);

            // Then
            assertThat(newScheduler).isNotNull();

            // Cleanup
            newScheduler.shutdown();
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update channel handler context")
        void shouldUpdateChannelHandlerContext() {
            // When
            scheduler.update(mockCtx);

            // Then
            // The update method should not throw any exception
            assertThat(scheduler).isNotNull();
        }

        @Test
        @DisplayName("Should handle null context update")
        void shouldHandleNullContextUpdate() {
            // When & Then
            // The update method should handle null gracefully or throw NPE
            // Let's test that it doesn't crash the scheduler
            scheduler.update(null);
            assertThat(scheduler).isNotNull();
        }
    }

    @Nested
    @DisplayName("Schedule Tests")
    class ScheduleTests {

        @Test
        @DisplayName("Should schedule task without key")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldScheduleTaskWithoutKey() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);

            // When
            scheduler.schedule(() -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should schedule task with key")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldScheduleTaskWithKey() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            // When
            scheduler.schedule(key, () -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should schedule task with immediate execution")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldScheduleTaskWithImmediateExecution() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);

            // When
            scheduler.schedule(() -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 0, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should handle multiple scheduled tasks")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleMultipleScheduledTasks() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger executionCount = new AtomicInteger(0);

            // When
            scheduler.schedule(() -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 50, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 150, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executionCount.get()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("ScheduleCallback Tests")
    class ScheduleCallbackTests {

        @BeforeEach
        void setUp() {
            scheduler.update(mockCtx);
        }

        @Test
        @DisplayName("Should schedule callback task")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldScheduleCallbackTask() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            // When
            scheduler.scheduleCallback(key, () -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should execute callback in event executor context")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldExecuteCallbackInEventExecutorContext() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean executedInExecutor = new AtomicBoolean(false);
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            // When
            scheduler.scheduleCallback(key, () -> {
                executedInExecutor.set(true);
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executedInExecutor.get()).isTrue();
            verify(mockExecutor, atLeastOnce()).execute(any(Runnable.class));
        }

        @Test
        @DisplayName("Should handle multiple callback tasks")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleMultipleCallbackTasks() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(3);
            AtomicInteger executionCount = new AtomicInteger(0);
            SchedulerKey key1 = new SchedulerKey(SchedulerKey.Type.PING, "session-1");
            SchedulerKey key2 = new SchedulerKey(SchedulerKey.Type.PING_TIMEOUT, "session-2");
            SchedulerKey key3 = new SchedulerKey(SchedulerKey.Type.ACK_TIMEOUT, "session-3");

            // When
            scheduler.scheduleCallback(key1, () -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 50, TimeUnit.MILLISECONDS);

            scheduler.scheduleCallback(key2, () -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 100, TimeUnit.MILLISECONDS);

            scheduler.scheduleCallback(key3, () -> {
                executionCount.incrementAndGet();
                latch.countDown();
            }, 150, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executionCount.get()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Cancel Tests")
    class CancelTests {

        @Test
        @DisplayName("Should cancel scheduled task")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldCancelScheduledTask() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            // When
            scheduler.schedule(key, () -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 500, TimeUnit.MILLISECONDS);

            // Cancel immediately
            scheduler.cancel(key);

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isFalse();
            assertThat(taskExecuted.get()).isFalse();
        }

        @Test
        @DisplayName("Should cancel callback task")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldCancelCallbackTask() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            scheduler.update(mockCtx);

            // When
            scheduler.scheduleCallback(key, () -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 500, TimeUnit.MILLISECONDS);

            // Cancel immediately
            scheduler.cancel(key);

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isFalse();
            assertThat(taskExecuted.get()).isFalse();
        }

        @Test
        @DisplayName("Should handle cancel of non-existent key")
        void shouldHandleCancelOfNonExistentKey() {
            // Given
            SchedulerKey nonExistentKey = new SchedulerKey(SchedulerKey.Type.PING, "non-existent");

            // When & Then
            // Cancelling non-existent key should not throw exception
            scheduler.cancel(nonExistentKey);
            assertThat(scheduler).isNotNull();
        }

        @Test
        @DisplayName("Should handle cancel of null key")
        void shouldHandleCancelOfNullKey() {
            // When & Then
            assertThatThrownBy(() -> scheduler.cancel(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("Should shutdown scheduler")
        void shouldShutdownScheduler() {
            // When
            scheduler.shutdown();

            // Then
            // Should not throw any exception
            assertThat(scheduler).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple shutdown calls")
        void shouldHandleMultipleShutdownCalls() {
            // When & Then
            // Multiple shutdown calls should not throw exception
            scheduler.shutdown();
            scheduler.shutdown();
            scheduler.shutdown();
            assertThat(scheduler).isNotNull();
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent scheduling")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldHandleConcurrentScheduling() throws InterruptedException {
            // Given
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger executionCount = new AtomicInteger(0);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "session-" + threadId);
                        scheduler.schedule(key, () -> {
                            executionCount.incrementAndGet();
                            completionLatch.countDown();
                        }, 100 + threadId * 10, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            startLatch.countDown();

            // Then
            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executionCount.get()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("Should handle concurrent cancellation")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldHandleConcurrentCancellation() throws InterruptedException {
            // Given
            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger executionCount = new AtomicInteger(0);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "session-" + threadId);

                        // Schedule and immediately cancel
                        scheduler.schedule(key, () -> {
                            executionCount.incrementAndGet();
                            completionLatch.countDown();
                        }, 200, TimeUnit.MILLISECONDS);

                        scheduler.cancel(key);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            startLatch.countDown();

            // Then
            boolean completed = completionLatch.await(3, TimeUnit.SECONDS);
            assertThat(completed).isFalse(); // Tasks should be cancelled
            assertThat(executionCount.get()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very short delays")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleVeryShortDelays() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);

            // When
            scheduler.schedule(() -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 1, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should handle zero delay")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleZeroDelay() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);

            // When
            scheduler.schedule(() -> {
                taskExecuted.set(true);
                latch.countDown();
            }, 0, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should handle negative delay")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldHandleNegativeDelay() throws InterruptedException {
            // Given
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean taskExecuted = new AtomicBoolean(false);

            // When
            scheduler.schedule(() -> {
                taskExecuted.set(true);
                latch.countDown();
            }, -100, TimeUnit.MILLISECONDS);

            // Then
            boolean completed = latch.await(1, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(taskExecuted.get()).isTrue();
        }

        @Test
        @DisplayName("Should handle null runnable")
        void shouldHandleNullRunnable() {
            // Given
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");

            // When & Then
            // Null runnable will cause NPE when the task executes, not when scheduled
            // We can't easily test this without waiting for execution, so we'll test that scheduling succeeds
            scheduler.schedule(key, null, 100, TimeUnit.MILLISECONDS);
            scheduler.schedule(null, 100, TimeUnit.MILLISECONDS);
            scheduler.scheduleCallback(key, null, 100, TimeUnit.MILLISECONDS);

            // The methods should not throw exception during scheduling
            assertThat(scheduler).isNotNull();
        }

        @Test
        @DisplayName("Should handle null time unit")
        void shouldHandleNullTimeUnit() {
            // Given
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.PING, "test-session");
            Runnable runnable = () -> {
            };

            // When & Then
            assertThatThrownBy(() -> scheduler.schedule(key, runnable, 100, null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> scheduler.schedule(runnable, 100, null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> scheduler.scheduleCallback(key, runnable, 100, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
