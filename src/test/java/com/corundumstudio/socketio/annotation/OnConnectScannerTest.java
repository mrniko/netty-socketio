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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.namespace.Namespace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OnConnectScanner class.
 * Tests the functionality of scanning and registering OnConnect annotation handlers.
 */
class OnConnectScannerTest extends AnnotationTestBase {

    private OnConnectScanner scanner;
    private Configuration config;
    private Namespace realNamespace;

    @Mock
    private Namespace mockNamespace;

    @Mock
    private SocketIOClient mockClient;

    private TestHandler testHandler;

    /**
     * Test handler class with OnConnect annotated methods.
     * Used to verify that the scanner correctly registers and invokes methods.
     * Each method tracks its own call statistics for precise validation.
     */
    public static class TestHandler {
        // OnConnect method tracking
        public boolean onConnectCalled = false;
        public SocketIOClient onConnectLastClient = null;
        public int onConnectCallCount = 0;

        // Invalid param method tracking
        public boolean onConnectInvalidParamCalled = false;
        public String onConnectInvalidParamLastParam = null;
        public int onConnectInvalidParamCallCount = 0;

        // Wrong param count method tracking
        public boolean onConnectWrongParamCountCalled = false;
        public SocketIOClient onConnectWrongParamCountLastClient = null;
        public String onConnectWrongParamCountLastExtra = null;
        public int onConnectWrongParamCountCallCount = 0;

        // Regular method tracking
        public boolean regularMethodCalled = false;
        public SocketIOClient regularMethodLastClient = null;
        public int regularMethodCallCount = 0;

        /**
         * Valid OnConnect method with correct signature.
         * Should be successfully registered and invoked.
         */
        @OnConnect
        public void onConnect(SocketIOClient client) {
            onConnectCalled = true;
            onConnectLastClient = client;
            onConnectCallCount++;
        }

        /**
         * Invalid OnConnect method with wrong parameter type.
         * Should cause validation to fail.
         */
        @OnConnect
        public void onConnectInvalidParam(String client) {
            onConnectInvalidParamCalled = true;
            onConnectInvalidParamLastParam = client;
            onConnectInvalidParamCallCount++;
        }

        /**
         * Invalid OnConnect method with wrong number of parameters.
         * Should cause validation to fail.
         */
        @OnConnect
        public void onConnectWrongParamCount(SocketIOClient client, String extra) {
            onConnectWrongParamCountCalled = true;
            onConnectWrongParamCountLastClient = client;
            onConnectWrongParamCountLastExtra = extra;
            onConnectWrongParamCountCallCount++;
        }

        /**
         * Method without OnConnect annotation.
         * Should not be registered by the scanner.
         */
        public void regularMethod(SocketIOClient client) {
            regularMethodCalled = true;
            regularMethodLastClient = client;
            regularMethodCallCount++;
        }

        /**
         * Resets all handler state for test isolation.
         */
        public void reset() {
            // Reset onConnect method state
            onConnectCalled = false;
            onConnectLastClient = null;
            onConnectCallCount = 0;

            // Reset invalid param method state
            onConnectInvalidParamCalled = false;
            onConnectInvalidParamLastParam = null;
            onConnectInvalidParamCallCount = 0;

            // Reset wrong param count method state
            onConnectWrongParamCountCalled = false;
            onConnectWrongParamCountLastClient = null;
            onConnectWrongParamCountLastExtra = null;
            onConnectWrongParamCountCallCount = 0;

            // Reset regular method state
            regularMethodCalled = false;
            regularMethodLastClient = null;
            regularMethodCallCount = 0;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scanner = new OnConnectScanner();
        testHandler = new TestHandler();
        
        // Create fresh configuration and namespace for each test
        config = newConfiguration();
        realNamespace = newNamespace(config);
        
        // Setup mock client with session ID
        when(mockClient.getSessionId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void testGetScanAnnotation() {
        // Test that the scanner returns the correct annotation type
        Class<? extends Annotation> annotationType = scanner.getScanAnnotation();
        assertEquals(OnConnect.class, annotationType);
    }

    @Test
    void testAddListenerSuccessfullyRegistersHandler() throws Exception {
        // Test that addListener correctly registers the handler with the namespace
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        // Execute the scanner
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that addConnectListener was called on the namespace
        verify(mockNamespace, times(1)).addConnectListener(any());
        
        // Verify that our test handler hasn't been called yet
        assertFalse(testHandler.onConnectCalled);
        assertEquals(0, testHandler.onConnectCallCount);
    }

    @Test
    void testAddListenerInvokesHandlerMethod() throws Exception {
        // Test that when a client connects, the registered handler method is actually invoked
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        // Register the handler using the scanner
        scanner.addListener(realNamespace, testHandler, method, annotation);

        // Verify initial state
        assertFalse(testHandler.onConnectCalled);
        assertEquals(0, testHandler.onConnectCallCount);

        // Simulate client connection by calling onConnect on the namespace
        realNamespace.onConnect(mockClient);

        // Verify that the handler method was actually called
        assertTrue(testHandler.onConnectCalled);
        assertEquals(mockClient, testHandler.onConnectLastClient);
        assertEquals(1, testHandler.onConnectCallCount);
        
        // Verify that other methods were not called
        assertFalse(testHandler.onConnectInvalidParamCalled);
        assertFalse(testHandler.onConnectWrongParamCountCalled);
        assertFalse(testHandler.regularMethodCalled);
    }

    @Test
    void testAddListenerHandlesMultipleConnections() throws Exception {
        // Test that the handler can handle multiple client connections
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        // Register the handler
        scanner.addListener(realNamespace, testHandler, method, annotation);

        // Simulate multiple client connections
        SocketIOClient client1 = mock(SocketIOClient.class);
        SocketIOClient client2 = mock(SocketIOClient.class);
        SocketIOClient client3 = mock(SocketIOClient.class);

        when(client1.getSessionId()).thenReturn(UUID.randomUUID());
        when(client2.getSessionId()).thenReturn(UUID.randomUUID());
        when(client3.getSessionId()).thenReturn(UUID.randomUUID());

        // Connect multiple clients
        realNamespace.onConnect(client1);
        realNamespace.onConnect(client2);
        realNamespace.onConnect(client3);

        // Verify the handler was called for each connection
        assertTrue(testHandler.onConnectCalled);
        assertEquals(3, testHandler.onConnectCallCount);
        assertEquals(client3, testHandler.onConnectLastClient); // Last client should be the most recent
        
        // Verify that other methods were not called
        assertEquals(0, testHandler.onConnectInvalidParamCallCount);
        assertEquals(0, testHandler.onConnectWrongParamCountCallCount);
        assertEquals(0, testHandler.regularMethodCallCount);
    }

    @Test
    void testValidateCorrectMethodSignature() throws Exception {
        // Test that validation passes for methods with correct signature
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateWrongParameterType() throws NoSuchMethodException {
        // Test that validation fails for methods with wrong parameter type
        Method method = TestHandler.class.getMethod("onConnectInvalidParam", String.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnConnect listener signature"));
        assertTrue(exception.getMessage().contains("onConnectInvalidParam"));
    }

    @Test
    void testValidateWrongParameterCount() throws NoSuchMethodException {
        // Test that validation fails for methods with wrong number of parameters
        Method method = TestHandler.class.getMethod("onConnectWrongParamCount", SocketIOClient.class, String.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnConnect listener signature"));
        assertTrue(exception.getMessage().contains("onConnectWrongParamCount"));
    }

    @Test
    void testValidateNoParameters() throws NoSuchMethodException {
        // Test that validation fails for methods with no parameters
        Method method = TestHandler.class.getMethod("reset");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnConnect listener signature"));
    }

    @Test
    void testAddListenerWithExceptionHandling() throws Exception {
        // Test that the scanner properly handles exceptions during method invocation
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        // Create a handler that throws an exception
        TestHandler exceptionHandler = new TestHandler() {
            @Override
            @OnConnect
            public void onConnect(SocketIOClient client) {
                super.onConnect(client); // Record the call
                throw new RuntimeException("Test exception");
            }
        };

        // Register the handler
        scanner.addListener(realNamespace, exceptionHandler, method, annotation);

        // Verify initial state
        assertFalse(exceptionHandler.onConnectCalled);

        // Simulate client connection - exceptions are caught by Namespace.onConnect
        // and passed to exceptionListener, so no exception should be thrown here
        realNamespace.onConnect(mockClient);
        
        // Verify that the handler method was called despite the exception
        assertTrue(exceptionHandler.onConnectCalled);
        assertEquals(mockClient, exceptionHandler.onConnectLastClient);
        assertEquals(1, exceptionHandler.onConnectCallCount);
        
        // The test passes if no exception is thrown, as the Namespace handles it
        // We can verify that the exception was logged by checking the logs if needed
    }

    @Test
    void testAddListenerIsolation() throws Exception {
        // Test that different handlers are isolated from each other
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        TestHandler handler1 = new TestHandler();
        TestHandler handler2 = new TestHandler();

        // Register both handlers
        scanner.addListener(realNamespace, handler1, method, annotation);
        scanner.addListener(realNamespace, handler2, method, annotation);

        // Verify initial state
        assertFalse(handler1.onConnectCalled);
        assertFalse(handler2.onConnectCalled);

        // Simulate client connection
        realNamespace.onConnect(mockClient);

        // Verify both handlers were called independently
        assertTrue(handler1.onConnectCalled);
        assertTrue(handler2.onConnectCalled);
        assertEquals(mockClient, handler1.onConnectLastClient);
        assertEquals(mockClient, handler2.onConnectLastClient);
        assertEquals(1, handler1.onConnectCallCount);
        assertEquals(1, handler2.onConnectCallCount);
        
        // Verify other methods were not called on either handler
        assertEquals(0, handler1.onConnectInvalidParamCallCount);
        assertEquals(0, handler1.regularMethodCallCount);
        assertEquals(0, handler2.onConnectInvalidParamCallCount);
        assertEquals(0, handler2.regularMethodCallCount);
    }

    @Test
    void testAddListenerWithNullValues() throws NoSuchMethodException {
        // Test that the scanner handles null values gracefully
        Method method = TestHandler.class.getMethod("onConnect", SocketIOClient.class);
        OnConnect annotation = method.getAnnotation(OnConnect.class);

        // Should not throw exception when adding listener
        assertThrows(NullPointerException.class, () -> 
            scanner.addListener(null, testHandler, method, annotation)
        );
    }
}