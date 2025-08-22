package com.corundumstudio.socketio.namespace;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/** Base test class for Namespace tests providing shared thread pool and utility methods. */
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
