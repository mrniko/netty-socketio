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
package com.corundumstudio.socketio.annotation;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.namespace.Namespace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScannerEngine class.
 * Tests the core functionality of scanning and registering annotation handlers.
 */
class ScannerEngineTest extends AnnotationTestBase {

    private ScannerEngine scannerEngine;
    private Configuration config;
    private Namespace realNamespace;

    @Mock
    private Namespace mockNamespace;

    @Mock
    private SocketIOClient mockClient;

    @Mock
    private AckRequest mockAckRequest;

    private TestHandler testHandler;

    /**
     * Test handler class with various annotated methods.
     * Used to verify that the scanner correctly registers and invokes methods.
     */
    public static class TestHandler {
        // OnConnect method tracking
        public boolean onConnectCalled = false;
        public SocketIOClient onConnectLastClient = null;
        public int onConnectCallCount = 0;

        // OnDisconnect method tracking
        public boolean onDisconnectCalled = false;
        public SocketIOClient onDisconnectLastClient = null;
        public int onDisconnectCallCount = 0;

        // OnEvent method tracking
        public boolean onEventCalled = false;
        public SocketIOClient onEventLastClient = null;
        public String onEventLastData = null;
        public int onEventCallCount = 0;



        // Regular method tracking
        public boolean regularMethodCalled = false;
        public int regularMethodCallCount = 0;

        @OnConnect
        public void onConnect(SocketIOClient client) {
            onConnectCalled = true;
            onConnectLastClient = client;
            onConnectCallCount++;
        }

        @OnDisconnect
        public void onDisconnect(SocketIOClient client) {
            onDisconnectCalled = true;
            onDisconnectLastClient = client;
            onDisconnectCallCount++;
        }

        @OnEvent("testEvent")
        public void onEvent(SocketIOClient client, String data) {
            onEventCalled = true;
            onEventLastClient = client;
            onEventLastData = data;
            onEventCallCount++;
        }

        public void regularMethod(SocketIOClient client) {
            regularMethodCalled = true;
            regularMethodCallCount++;
        }

        /**
         * Resets all handler state for test isolation.
         */
        public void reset() {
            onConnectCalled = false;
            onConnectLastClient = null;
            onConnectCallCount = 0;

            onDisconnectCalled = false;
            onDisconnectLastClient = null;
            onDisconnectCallCount = 0;

            onEventCalled = false;
            onEventLastClient = null;
            onEventLastData = null;
            onEventCallCount = 0;

            regularMethodCalled = false;
            regularMethodCallCount = 0;
        }
    }

    /**
     * Subclass that implements some methods from parent interface/class
     */
    public static class SubTestHandler extends TestHandler {
        public boolean subOnConnectCalled = false;
        public int subOnConnectCallCount = 0;

        @Override
        @OnConnect
        public void onConnect(SocketIOClient client) {
            super.onConnect(client);
            subOnConnectCalled = true;
            subOnConnectCallCount++;
        }
    }

    /**
     * Interface with annotated methods
     */
    public interface TestInterface {
        @OnConnect
        void interfaceOnConnect(SocketIOClient client);
    }

    /**
     * Implementation of test interface
     */
    public static class TestInterfaceImpl implements TestInterface {
        public boolean interfaceOnConnectCalled = false;
        public int interfaceOnConnectCallCount = 0;

        @Override
        public void interfaceOnConnect(SocketIOClient client) {
            interfaceOnConnectCalled = true;
            interfaceOnConnectCallCount++;
        }
    }

    private AutoCloseable closeableMocks;

    @BeforeEach
    void setUp() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        scannerEngine = new ScannerEngine();
        testHandler = new TestHandler();

        // Create fresh configuration and namespace for each test
        config = newConfiguration();
        realNamespace = newNamespace(config);

        // Setup mock client with session ID
        when(mockClient.getSessionId()).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeableMocks.close();
    }

    @Test
    void testScanBasicAnnotatedMethods() {
        // Test that scan correctly identifies and registers annotated methods
        class SimpleTestHandler {
            public boolean onConnectCalled = false;
            public boolean onDisconnectCalled = false;
            public boolean regularMethodCalled = false;
            public SocketIOClient lastClient = null;
            public int connectCallCount = 0;
            public int disconnectCallCount = 0;

            @OnConnect
            public void onConnect(SocketIOClient client) {
                onConnectCalled = true;
                lastClient = client;
                connectCallCount++;
            }

            @OnDisconnect
            public void onDisconnect(SocketIOClient client) {
                onDisconnectCalled = true;
                lastClient = client;
                disconnectCallCount++;
            }

            public void regularMethod(SocketIOClient client) {
                regularMethodCalled = true;
            }
        }

        SimpleTestHandler handler = new SimpleTestHandler();
        scannerEngine.scan(realNamespace, handler, SimpleTestHandler.class);

        // Verify initial state
        assertFalse(handler.onConnectCalled);
        assertFalse(handler.onDisconnectCalled);
        assertFalse(handler.regularMethodCalled);

        // Trigger events and verify handlers are called
        realNamespace.onConnect(mockClient);
        assertTrue(handler.onConnectCalled);
        assertEquals(mockClient, handler.lastClient);
        assertEquals(1, handler.connectCallCount);

        realNamespace.onDisconnect(mockClient);
        assertTrue(handler.onDisconnectCalled);
        assertEquals(mockClient, handler.lastClient);
        assertEquals(1, handler.disconnectCallCount);

        // Regular method should not be called
        assertFalse(handler.regularMethodCalled);
    }

    @Test
    void testScanWithMockNamespace() {
        // Test that scan properly calls the namespace methods using simple handler
        class SimpleHandler {
            @OnConnect
            public void onConnect(SocketIOClient client) {}

            @OnDisconnect  
            public void onDisconnect(SocketIOClient client) {}
        }

        SimpleHandler handler = new SimpleHandler();
        scannerEngine.scan(mockNamespace, handler, SimpleHandler.class);

        // Verify that appropriate listeners were added to the namespace
        verify(mockNamespace, times(1)).addConnectListener(any());
        verify(mockNamespace, times(1)).addDisconnectListener(any());
        // No OnEvent methods, so no addEventListener calls
        verify(mockNamespace, never()).addEventListener(any(), any(), any());
    }

    @Test
    void testScanPrivateMethod() {
        // Test that scan can handle private annotated methods
        class PrivateMethodHandler {
            public boolean publicOnConnectCalled = false;
            public boolean privateOnConnectCalled = false;
            public int publicCallCount = 0;
            public int privateCallCount = 0;

            @OnConnect
            public void publicOnConnect(SocketIOClient client) {
                publicOnConnectCalled = true;
                publicCallCount++;
            }

            @OnConnect
            private void privateOnConnect(SocketIOClient client) {
                privateOnConnectCalled = true;
                privateCallCount++;
            }
        }

        PrivateMethodHandler handler = new PrivateMethodHandler();
        scannerEngine.scan(realNamespace, handler, PrivateMethodHandler.class);

        // Trigger connect event
        realNamespace.onConnect(mockClient);

        // Both public and private methods should be called
        assertTrue(handler.publicOnConnectCalled);
        assertTrue(handler.privateOnConnectCalled);
        assertEquals(1, handler.publicCallCount);
        assertEquals(1, handler.privateCallCount);
    }

    @Test
    void testScanInheritanceHierarchy() {
        // Test that scan correctly handles inheritance without method override
        class BaseHandler {
            public boolean baseConnectCalled = false;
            public int baseConnectCallCount = 0;
            
            @OnConnect
            public void baseOnConnect(SocketIOClient client) {
                baseConnectCalled = true;
                baseConnectCallCount++;
            }
        }
        
        class DerivedHandler extends BaseHandler {
            public boolean derivedDisconnectCalled = false;
            public int derivedDisconnectCallCount = 0;
            
            @OnDisconnect
            public void derivedOnDisconnect(SocketIOClient client) {
                derivedDisconnectCalled = true;
                derivedDisconnectCallCount++;
            }
        }

        DerivedHandler derivedHandler = new DerivedHandler();
        scannerEngine.scan(realNamespace, derivedHandler, DerivedHandler.class);

        // Trigger events
        realNamespace.onConnect(mockClient);
        realNamespace.onDisconnect(mockClient);

        // Methods from inheritance hierarchy should be called
        assertTrue(derivedHandler.baseConnectCalled);
        assertTrue(derivedHandler.derivedDisconnectCalled);
        assertEquals(1, derivedHandler.baseConnectCallCount);
        assertEquals(1, derivedHandler.derivedDisconnectCallCount);
    }

    @Test
    void testScanInterfaceAnnotations() {
        // Test that scan correctly handles interface annotations
        TestInterfaceImpl interfaceImpl = new TestInterfaceImpl();
        scannerEngine.scan(realNamespace, interfaceImpl, TestInterface.class);

        // Trigger connect event
        realNamespace.onConnect(mockClient);

        // Interface method should be called
        assertTrue(interfaceImpl.interfaceOnConnectCalled);
        assertEquals(1, interfaceImpl.interfaceOnConnectCallCount);
    }

    @Test
    void testScanWithDifferentObjectAndClass() {
        // Test that scan handles cases where object class differs from scanned class
        TestInterfaceImpl interfaceImpl = new TestInterfaceImpl();
        
        // Scan interface but use implementation object
        scannerEngine.scan(realNamespace, interfaceImpl, TestInterface.class);

        // Trigger connect event
        realNamespace.onConnect(mockClient);

        // Method should be found and called
        assertTrue(interfaceImpl.interfaceOnConnectCalled);
        assertEquals(1, interfaceImpl.interfaceOnConnectCallCount);
    }

    @Test
    void testScanWithNoMatchingSimilarMethod() {
        // Test behavior when no similar method is found in the object
        TestHandler handler = new TestHandler();
        
        // Create a mock class that has methods but object doesn't have similar ones
        class MockClass {
            @OnConnect
            public void nonExistentMethod(SocketIOClient client) {
                // This method doesn't exist in TestHandler
            }
        }

        // This should not throw an exception, but should log a warning
        scannerEngine.scan(realNamespace, handler, MockClass.class);

        // Verify no listeners were added for the non-existent method
        realNamespace.onConnect(mockClient);
        assertFalse(handler.onConnectCalled);
    }

    @Test
    void testScanWithValidationErrors() {
        // Test that scan handles validation errors gracefully
        class InvalidHandler {
            @OnConnect
            public void invalidOnConnect(String wrongParam) {
                // Wrong parameter type - should cause validation error
            }
        }

        InvalidHandler invalidHandler = new InvalidHandler();
        
        // Should throw IllegalArgumentException during validation
        assertThrows(IllegalArgumentException.class, () -> {
            scannerEngine.scan(realNamespace, invalidHandler, InvalidHandler.class);
        });
    }

    @Test
    void testScanWithNullNamespace() {
        // Test that scan handles null namespace gracefully
        assertThrows(NullPointerException.class, () -> {
            scannerEngine.scan(null, testHandler, TestHandler.class);
        });
    }

    @Test
    void testScanWithNullObject() {
        // Test that scan handles null object gracefully
        assertThrows(NullPointerException.class, () -> {
            scannerEngine.scan(realNamespace, null, TestHandler.class);
        });
    }

    @Test
    void testScanWithNullClass() {
        // Test that scan handles null class gracefully
        assertThrows(NullPointerException.class, () -> {
            scannerEngine.scan(realNamespace, testHandler, null);
        });
    }

    @Test
    void testScanEmptyClass() {
        // Test that scan handles classes with no methods
        class EmptyClass {
            // No methods
        }

        EmptyClass emptyObject = new EmptyClass();
        
        // Should not throw exception
        scannerEngine.scan(realNamespace, emptyObject, EmptyClass.class);
        
        // Test should complete without exception, which means success
        // We can't easily verify no listeners were added to realNamespace,
        // but the lack of exceptions indicates correct behavior
    }

    @Test
    void testScanMultipleAnnotationsOnSameMethod() {
        // Test class with method having multiple annotations (if possible)
        class MultiAnnotationHandler {
            public boolean called = false;
            
            @OnConnect
            public void multiAnnotated(SocketIOClient client) {
                called = true;
            }
        }

        MultiAnnotationHandler handler = new MultiAnnotationHandler();
        scannerEngine.scan(realNamespace, handler, MultiAnnotationHandler.class);

        // Trigger connect event
        realNamespace.onConnect(mockClient);
        assertTrue(handler.called);
    }

    @Test
    void testScanRecursiveInheritance() {
        // Test that scan properly handles recursive scanning of parent classes
        class GrandParent {
            public boolean grandParentCalled = false;
            
            @OnConnect
            public void grandParentMethod(SocketIOClient client) {
                grandParentCalled = true;
            }
        }

        class Parent extends GrandParent {
            public boolean parentCalled = false;
            
            @OnDisconnect
            public void parentMethod(SocketIOClient client) {
                parentCalled = true;
            }
        }

        class Child extends Parent {
            // No additional annotated methods in child
        }

        Child child = new Child();
        scannerEngine.scan(realNamespace, child, Child.class);

        // All methods from hierarchy should be registered
        realNamespace.onConnect(mockClient);
        assertTrue(child.grandParentCalled);

        realNamespace.onDisconnect(mockClient);
        assertTrue(child.parentCalled);
    }

    @Test
    void testScanPerformanceWithManyMethods() {
        // Test scan performance with a class containing many methods
        @SuppressWarnings("unused")
        class ManyMethodsHandler {
            public int callCount = 0;
            
            @OnConnect public void method1(SocketIOClient client) { callCount++; }
            @OnConnect public void method2(SocketIOClient client) { callCount++; }
            @OnConnect public void method3(SocketIOClient client) { callCount++; }
            @OnConnect public void method4(SocketIOClient client) { callCount++; }
            @OnConnect public void method5(SocketIOClient client) { callCount++; }
            
            // Non-annotated methods - used for performance testing
            public void regularMethod1() {}
            public void regularMethod2() {}
            public void regularMethod3() {}
            public void regularMethod4() {}
            public void regularMethod5() {}
        }

        ManyMethodsHandler handler = new ManyMethodsHandler();
        
        // Should complete without timeout or excessive delay
        long startTime = System.currentTimeMillis();
        scannerEngine.scan(realNamespace, handler, ManyMethodsHandler.class);
        long endTime = System.currentTimeMillis();
        
        // Should complete in reasonable time (less than 1 second)
        assertTrue(endTime - startTime < 1000, "Scan took too long: " + (endTime - startTime) + "ms");

        // All annotated methods should be registered
        realNamespace.onConnect(mockClient);
        assertEquals(5, handler.callCount);
    }

    @Test
    void testScanThreadSafety() throws InterruptedException {
        // Test that scan can be called concurrently without issues
        final int threadCount = 5;
        final boolean[] completed = new boolean[threadCount];
        final Thread[] threads = new Thread[threadCount];

        class ThreadTestHandler {
            public boolean onConnectCalled = false;
            
            @OnConnect
            public void onConnect(SocketIOClient client) {
                onConnectCalled = true;
            }
        }

        final ThreadTestHandler[] handlers = new ThreadTestHandler[threadCount];

        // Create threads that scan different handlers
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            handlers[i] = new ThreadTestHandler();
            threads[i] = new Thread(() -> {
                try {
                    // Each thread uses its own namespace to avoid conflicts
                    Configuration threadConfig = newConfiguration();
                    Namespace threadNamespace = newNamespace(threadConfig);
                    scannerEngine.scan(threadNamespace, handlers[index], ThreadTestHandler.class);
                    completed[index] = true;
                } catch (Exception e) {
                    // Mark as failed
                    completed[index] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }

        // Verify all threads completed successfully
        for (int i = 0; i < threadCount; i++) {
            assertTrue(completed[i], "Thread " + i + " did not complete successfully");
        }
    }
}
