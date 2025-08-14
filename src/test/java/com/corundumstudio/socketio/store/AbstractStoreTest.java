package com.corundumstudio.socketio.store;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Abstract base class for store tests providing common test methods and utilities
 */
public abstract class AbstractStoreTest {

    protected Store store;
    protected UUID sessionId;
    protected GenericContainer<?> container;

    @Before
    public void setUp() throws Exception {
        sessionId = UUID.randomUUID();
        container = createContainer();
        container.start();
        store = createStore(sessionId);
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            // Clean up store data
            cleanupStore();
        }
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    /**
     * Create the container for testing
     */
    protected abstract GenericContainer<?> createContainer();

    /**
     * Create the store instance for testing
     */
    protected abstract Store createStore(UUID sessionId) throws Exception;

    /**
     * Clean up store data after tests
     */
    protected abstract void cleanupStore();

    @Test
    public void testBasicOperations() {
        // Test set and get
        store.set("key1", "value1");
        store.set("key2", 123);
        store.set("key3", true);

        assertEquals("value1", store.get("key1"));
        assertTrue(store.get("key2") instanceof Integer && ((Integer) store.get("key2")).equals(123));
        assertEquals(true, store.get("key3"));

        // Test has
        assertTrue(store.has("key1"));
        assertTrue(store.has("key2"));
        assertTrue(store.has("key3"));
        assertFalse(store.has("nonexistent"));

        // Test del
        store.del("key1");
        assertFalse(store.has("key1"));
        assertNull(store.get("key1"));
    }

    @Test
    public void testNullValues() {
        assertThrows(NullPointerException.class, () -> {
            store.set("nullKey", null);
        });
    }

    @Test
    public void testComplexObjects() {
        TestObject testObj = new TestObject("test", 42);
        store.set("complexKey", testObj);
        
        TestObject retrieved = store.get("complexKey");
        Assertions.assertThat(retrieved).isNotNull();
        Assertions.assertThat(retrieved.getName()).isEqualTo("test");
        Assertions.assertThat(retrieved.getValue()).isEqualTo(42);
    }

    @Test
    public void testOverwriteValues() {
        store.set("overwriteKey", "original");
        Assertions.assertThat((String) store.get("overwriteKey")).isEqualTo("original");
        
        store.set("overwriteKey", "updated");
        Assertions.assertThat((String) store.get("overwriteKey")).isEqualTo("updated");
    }

    @Test
    public void testDeleteNonExistentKey() {
        // Should not throw exception
        store.del("nonexistent");
        Assertions.assertThat(store.has("nonexistent")).isFalse();
    }

    @Test
    public void testGetNonExistentKey() {
        Assertions.assertThat((String) store.get("nonexistent")).isNull();
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread" + threadId + "_key" + j;
                    String value = "value" + threadId + "_" + j;
                    store.set(key, value);
                    Assertions.assertThat((String) store.get(key)).isEqualTo(value);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all values were set correctly
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < operationsPerThread; j++) {
                String key = "thread" + i + "_key" + j;
                String expectedValue = "value" + i + "_" + j;
                Assertions.assertThat((String) store.get(key)).isEqualTo(expectedValue);
            }
        }
    }

    /**
     * Test object for complex object testing
     */
    public static class TestObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return value == that.value && (name != null ? name.equals(that.name) : that.name == null);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + value;
            return result;
        }
    }
}
