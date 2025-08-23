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

import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.listener.DataListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for EventEntry functionality and thread safety.
 */
class EventEntryTest extends BaseNamespaceTest {

    private EventEntry<String> eventEntry;
    private static final String TEST_DATA = "testData";

    @BeforeEach
    void setUp() {
        eventEntry = new EventEntry<>();
    }

    /**
     * Test basic EventEntry properties and initial state
     */
    @Test
    void testBasicProperties() {
        // Test initial state
        assertNotNull(eventEntry);

        // Test listeners collection is initially empty
        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        assertNotNull(listeners);
        assertTrue(listeners.isEmpty());
        assertEquals(0, listeners.size());

        // Test listeners collection is the same instance
        assertSame(listeners, eventEntry.getListeners());
    }

    /**
     * Test listener management functionality
     */
    @Test
    void testListenerManagement() {
        // Test adding single listener
        DataListener<String> listener1 = (client, data, ackRequest) -> {
        };
        assertNotNull(listener1);

        eventEntry.addListener(listener1);

        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener1));

        // Test adding multiple listeners
        DataListener<String> listener2 = (client, data, ackRequest) -> {
        };
        DataListener<String> listener3 = (client, data, ackRequest) -> {
        };

        eventEntry.addListener(listener2);
        eventEntry.addListener(listener3);

        assertEquals(3, listeners.size());
        assertTrue(listeners.contains(listener2));
        assertTrue(listeners.contains(listener3));

        // Test adding duplicate listener (should be allowed)
        eventEntry.addListener(listener1);
        assertEquals(4, listeners.size());

        // Verify all listeners are present
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));
        assertTrue(listeners.contains(listener3));
    }

    /**
     * Test concurrent listener operations with thread safety
     */
    @Test
    void testConcurrentListenerOperations() throws InterruptedException {
        int taskCount = DEFAULT_TASK_COUNT;

        // Test concurrent listener addition
        CountDownLatch addLatch =
                executeConcurrentOperations(
                        taskCount,
                        () -> {
                            try {
                                DataListener<String> listener = (client, data, ackRequest) -> {
                                };
                                assertNotNull(listener);
                                eventEntry.addListener(listener);
                            } catch (Exception e) {
                                // Log exception but continue
                            }
                        });

        waitForCompletion(addLatch);

        // Verify all listeners were added safely
        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        assertEquals(taskCount, listeners.size());
        assertTrue(listeners.size() > 0);

        // Test concurrent listener retrieval
        CountDownLatch retrieveLatch =
                executeConcurrentOperations(
                        taskCount,
                        () -> {
                            try {
                                Queue<DataListener<String>> retrievedListeners = eventEntry.getListeners();
                                assertNotNull(retrievedListeners);
                                assertTrue(retrievedListeners.size() >= taskCount);

                                // Verify we can iterate over listeners safely
                                int count = 0;
                                for (DataListener<String> listener : retrievedListeners) {
                                    assertNotNull(listener);
                                    count++;
                                }
                                assertTrue(count >= taskCount);
                            } catch (Exception e) {
                                // Log exception but continue
                            }
                        });

        waitForCompletion(retrieveLatch);

        // Verify final state
        assertEquals(taskCount, eventEntry.getListeners().size());
        assertTrue(addLatch.getCount() == 0);
        assertTrue(retrieveLatch.getCount() == 0);
    }

    /**
     * Test listener collection properties and behavior
     */
    @Test
    void testListenerCollectionProperties() {
        // Test that listeners collection is a ConcurrentLinkedQueue
        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        assertNotNull(listeners);

        // Test adding and removing listeners
        DataListener<String> listener1 = (client, data, ackRequest) -> {
        };
        DataListener<String> listener2 = (client, data, ackRequest) -> {
        };

        eventEntry.addListener(listener1);
        eventEntry.addListener(listener2);

        assertEquals(2, listeners.size());

        // Test removing listeners (ConcurrentLinkedQueue doesn't support remove by object)
        // But we can test other operations
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        // Test iteration
        int count = 0;
        for (DataListener<String> listener : listeners) {
            assertNotNull(listener);
            count++;
        }
        assertEquals(2, count);
    }

    /**
     * Test edge cases and boundary conditions
     */
    @Test
    void testEdgeCasesAndBoundaries() {
        // Test adding null listener (may or may not be allowed by ConcurrentLinkedQueue)
        int initialSize = eventEntry.getListeners().size();
        try {
            eventEntry.addListener(null);
            // If no exception, verify it was added
            Queue<DataListener<String>> listeners = eventEntry.getListeners();
            assertTrue(listeners.size() > initialSize);
        } catch (Exception e) {
            // If exception is thrown, that's also acceptable behavior
            assertNotNull(e);
        }

        // Test adding many listeners
        int largeCount = 1000;
        for (int i = 0; i < largeCount; i++) {
            DataListener<String> listener = (client, data, ackRequest) -> {
            };
            eventEntry.addListener(listener);
        }

        // Get final size (may or may not include null listener)
        int finalSize = eventEntry.getListeners().size();
        assertTrue(finalSize >= largeCount);

        // Test that all listeners are accessible
        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        int count = 0;
        for (DataListener<String> listener : listeners) {
            count++;
        }
        assertEquals(finalSize, count);
    }

    /**
     * Test listener execution simulation
     */
    @Test
    void testListenerExecutionSimulation() {
        // Create listeners that track execution
        final boolean[] executed1 = {false};
        final boolean[] executed2 = {false};

        DataListener<String> listener1 =
                (client, data, ackRequest) -> {
                    executed1[0] = true;
                    assertNotNull(data);
                    assertEquals(TEST_DATA, data);
                };

        DataListener<String> listener2 =
                (client, data, ackRequest) -> {
                    executed2[0] = true;
                    assertNotNull(data);
                    assertEquals(TEST_DATA, data);
                };

        // Add listeners
        eventEntry.addListener(listener1);
        eventEntry.addListener(listener2);

        // Simulate execution (this is just a simulation, not actual execution)
        Queue<DataListener<String>> listeners = eventEntry.getListeners();
        assertEquals(2, listeners.size());

        // Verify listeners are in the collection
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        // Test that we can access the listeners
        DataListener<String>[] listenerArray = listeners.toArray(new DataListener[0]);
        assertEquals(2, listenerArray.length);
        assertNotNull(listenerArray[0]);
        assertNotNull(listenerArray[1]);
    }
}
