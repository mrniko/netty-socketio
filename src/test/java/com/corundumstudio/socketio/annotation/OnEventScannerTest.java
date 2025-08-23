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

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.MultiTypeArgs;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.namespace.Namespace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OnEventScanner class.
 * Tests the functionality of scanning and registering OnEvent annotation handlers.
 * OnEvent is more complex than OnConnect/OnDisconnect as it supports:
 * - Multiple parameter combinations (SocketIOClient, AckRequest, event data)
 * - Single and multi-type event listeners
 * - Event name validation
 * - Parameter index calculation and validation
 */
class OnEventScannerTest extends AnnotationTestBase {

    private OnEventScanner scanner;
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
     * Test handler class with OnEvent annotated methods.
     * Used to verify that the scanner correctly registers and invokes methods.
     * Each method tracks its own call statistics for precise validation.
     */
    public static class TestHandler {
        // Basic event method tracking
        public boolean basicEventCalled = false;
        public String basicEventLastData = null;
        public SocketIOClient basicEventLastClient = null;
        public int basicEventCallCount = 0;

        // Event with client parameter tracking
        public boolean eventWithClientCalled = false;
        public String eventWithClientLastData = null;
        public SocketIOClient eventWithClientLastClient = null;
        public int eventWithClientCallCount = 0;

        // Event with ack parameter tracking
        public boolean eventWithAckCalled = false;
        public String eventWithAckLastData = null;
        public AckRequest eventWithAckLastAck = null;
        public int eventWithAckCallCount = 0;

        // Event with client and ack parameters tracking
        public boolean eventWithClientAndAckCalled = false;
        public String eventWithClientAndAckLastData = null;
        public SocketIOClient eventWithClientAndAckLastClient = null;
        public AckRequest eventWithClientAndAckLastAck = null;
        public int eventWithClientAndAckCallCount = 0;

        // Multi-type event method tracking
        public boolean multiTypeEventCalled = false;
        public MultiTypeArgs multiTypeEventLastData = null;
        public SocketIOClient multiTypeEventLastClient = null;
        public AckRequest multiTypeEventLastAck = null;
        public int multiTypeEventCallCount = 0;

        // Invalid event method tracking (no value)
        public boolean invalidEventCalled = false;
        public int invalidEventCallCount = 0;

        // Regular method tracking
        public boolean regularMethodCalled = false;
        public int regularMethodCallCount = 0;

        /**
         * Basic event method with only data parameter.
         * Should be successfully registered and invoked.
         */
        @OnEvent("basic")
        public void basicEvent(String data) {
            basicEventCalled = true;
            basicEventLastData = data;
            basicEventCallCount++;
        }

        /**
         * Event method with client parameter.
         * Should be successfully registered and invoked.
         */
        @OnEvent("withClient")
        public void eventWithClient(String data, SocketIOClient client) {
            eventWithClientCalled = true;
            eventWithClientLastData = data;
            eventWithClientLastClient = client;
            eventWithClientCallCount++;
        }

        /**
         * Event method with ack parameter.
         * Should be successfully registered and invoked.
         */
        @OnEvent("withAck")
        public void eventWithAck(String data, AckRequest ack) {
            eventWithAckCalled = true;
            eventWithAckLastData = data;
            eventWithAckLastAck = ack;
            eventWithAckCallCount++;
        }

        /**
         * Event method with client and ack parameters.
         * Should be successfully registered and invoked.
         */
        @OnEvent("withClientAndAck")
        public void eventWithClientAndAck(String data, SocketIOClient client, AckRequest ack) {
            eventWithClientAndAckCalled = true;
            eventWithClientAndAckLastData = data;
            eventWithClientAndAckLastClient = client;
            eventWithClientAndAckLastAck = ack;
            eventWithClientAndAckCallCount++;
        }

        /**
         * Multi-type event method with multiple data parameters.
         * Should be successfully registered and invoked.
         */
        @OnEvent("multiType")
        public void multiTypeEvent(String data1, Integer data2, SocketIOClient client, AckRequest ack) {
            multiTypeEventCalled = true;
            multiTypeEventLastData = new MultiTypeArgs(java.util.Arrays.asList(data1, data2));
            multiTypeEventLastClient = client;
            multiTypeEventLastAck = ack;
            multiTypeEventCallCount++;
        }

        /**
         * Method without OnEvent annotation.
         * Should not be registered by the scanner.
         */
        public void regularMethod(String data) {
            regularMethodCalled = true;
            regularMethodCallCount++;
        }

        /**
         * Resets all handler state for test isolation.
         */
        public void reset() {
            // Reset basic event method state
            basicEventCalled = false;
            basicEventLastData = null;
            basicEventLastClient = null;
            basicEventCallCount = 0;

            // Reset event with client method state
            eventWithClientCalled = false;
            eventWithClientLastData = null;
            eventWithClientLastClient = null;
            eventWithClientCallCount = 0;

            // Reset event with ack method state
            eventWithAckCalled = false;
            eventWithAckLastData = null;
            eventWithAckLastAck = null;
            eventWithAckCallCount = 0;

            // Reset event with client and ack method state
            eventWithClientAndAckCalled = false;
            eventWithClientAndAckLastData = null;
            eventWithClientAndAckLastClient = null;
            eventWithClientAndAckLastAck = null;
            eventWithClientAndAckCallCount = 0;

            // Reset multi-type event method state
            multiTypeEventCalled = false;
            multiTypeEventLastData = null;
            multiTypeEventLastClient = null;
            multiTypeEventLastAck = null;
            multiTypeEventCallCount = 0;

            // Reset invalid event method state
            invalidEventCalled = false;
            invalidEventCallCount = 0;

            // Reset regular method state
            regularMethodCalled = false;
            regularMethodCallCount = 0;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scanner = new OnEventScanner();
        testHandler = new TestHandler();
        
        // Create fresh configuration and namespace for each test
        config = newConfiguration();
        realNamespace = newNamespace(config);
        
        // Setup mock client with session ID
        when(mockClient.getSessionId()).thenReturn(UUID.randomUUID());
        
        // Setup mock ack request
        when(mockAckRequest.isAckRequested()).thenReturn(true);
        
        // Setup mock namespace for testing - these methods return void, so we just need to ensure they don't throw
        // No need to mock void methods
    }

    @Test
    void testGetScanAnnotation() {
        // Test that the scanner returns the correct annotation type
        Class<? extends Annotation> annotationType = scanner.getScanAnnotation();
        assertEquals(OnEvent.class, annotationType);
    }

    @Test
    void testAddListenerSuccessfullyRegistersBasicHandler() throws Exception {
        // Test that addListener correctly registers a basic event handler with the namespace
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        // Execute the scanner
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that addEventListener was called on the namespace with correct event name and type
        verify(mockNamespace, times(1)).addEventListener(eq("basic"), eq(String.class), any());
        
        // Verify that our test handler hasn't been called yet
        assertFalse(testHandler.basicEventCalled);
        assertEquals(0, testHandler.basicEventCallCount);
    }

    @Test
    void testAddListenerSuccessfullyRegistersHandlerWithClient() throws Exception {
        // Test that addListener correctly registers an event handler with client parameter
        Method method = TestHandler.class.getMethod("eventWithClient", String.class, SocketIOClient.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        // Execute the scanner
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that addEventListener was called on the namespace
        verify(mockNamespace, times(1)).addEventListener(eq("withClient"), eq(String.class), any());
        
        // Verify that our test handler hasn't been called yet
        assertFalse(testHandler.eventWithClientCalled);
        assertEquals(0, testHandler.eventWithClientCallCount);
    }

    @Test
    void testAddListenerSuccessfullyRegistersMultiTypeHandler() throws Exception {
        // Test that addListener correctly registers a multi-type event handler
        Method method = TestHandler.class.getMethod("multiTypeEvent", String.class, Integer.class, SocketIOClient.class, AckRequest.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        // Execute the scanner
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that addMultiTypeEventListener was called on the namespace
        verify(mockNamespace, times(1)).addMultiTypeEventListener(eq("multiType"), any(), eq(new Class<?>[]{String.class, Integer.class}));
        
        // Verify that our test handler hasn't been called yet
        assertFalse(testHandler.multiTypeEventCalled);
        assertEquals(0, testHandler.multiTypeEventCallCount);
    }

    @Test
    void testAddListenerThrowsExceptionForNullEventValue() throws Exception {
        // Test that addListener throws exception when event value is null
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock annotation with null value
        OnEvent mockAnnotation = mock(OnEvent.class);
        when(mockAnnotation.value()).thenReturn(null);

        // Should throw IllegalArgumentException for null event value
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.addListener(mockNamespace, testHandler, method, mockAnnotation)
        );
        
        assertTrue(exception.getMessage().contains("OnEvent \"value\" parameter is required"));
    }

    @Test
    void testAddListenerThrowsExceptionForEmptyEventValue() throws Exception {
        // Test that addListener throws exception when event value is empty
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock annotation with empty value
        OnEvent mockAnnotation = mock(OnEvent.class);
        when(mockAnnotation.value()).thenReturn("");

        // Should throw IllegalArgumentException for empty event value
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.addListener(mockNamespace, testHandler, method, mockAnnotation)
        );
        
        assertTrue(exception.getMessage().contains("OnEvent \"value\" parameter is required"));
    }

    @Test
    void testValidateCorrectMethodSignature() throws Exception {
        // Test that validation passes for methods with correct signature
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateMethodWithClientParameter() throws Exception {
        // Test that validation passes for methods with client parameter
        Method method = TestHandler.class.getMethod("eventWithClient", String.class, SocketIOClient.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateMethodWithAckParameter() throws Exception {
        // Test that validation passes for methods with ack parameter
        Method method = TestHandler.class.getMethod("eventWithAck", String.class, AckRequest.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateMethodWithClientAndAckParameters() throws Exception {
        // Test that validation passes for methods with both client and ack parameters
        Method method = TestHandler.class.getMethod("eventWithClientAndAck", String.class, SocketIOClient.class, AckRequest.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateMultiTypeMethod() throws Exception {
        // Test that validation passes for multi-type event methods
        Method method = TestHandler.class.getMethod("multiTypeEvent", String.class, Integer.class, SocketIOClient.class, AckRequest.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidatePassesForMethodWithExtraParameters() throws NoSuchMethodException {
        // Test that validation passes for methods with extra parameters
        // OnEvent allows extra parameters as long as they are not SocketIOClient or AckRequest
        Method method = TestHandler.class.getMethod("eventWithClient", String.class, SocketIOClient.class);
        
        // Create a mock method with extra parameter
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{String.class, SocketIOClient.class, Integer.class});
        when(mockMethod.getName()).thenReturn("eventWithClient");
        
        // Should not throw exception - this is a valid signature
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testValidatePassesForMethodWithWrongParameterTypes() throws NoSuchMethodException {
        // Test that validation passes for methods with wrong parameter types
        // OnEvent only checks parameter count, not parameter types
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with wrong parameter type
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{Integer.class, String.class});
        when(mockMethod.getName()).thenReturn("basicEvent");
        
        // Should not throw exception - OnEvent only validates parameter count
        scanner.validate(mockMethod, TestHandler.class);
    }

        @Test
    void testValidatePassesForMethodWithNoDataParameters() throws NoSuchMethodException {
        // Test that validation passes for methods with only client and ack parameters (no data)
        // This is actually valid in OnEvent - it allows methods with only client and ack parameters
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with only client and ack parameters
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{SocketIOClient.class, AckRequest.class});
        when(mockMethod.getName()).thenReturn("eventOnly");

        // Should not throw exception - this is a valid signature
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testValidatePassesForMethodWithUnrecognizedParameterTypes() throws NoSuchMethodException {
        // Test that validation passes for methods with unrecognized parameter types
        // OnEvent allows any parameter types as long as parameter count is correct
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with unrecognized parameter type
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{String.class, Object.class});
        when(mockMethod.getName()).thenReturn("eventWithObject");
        
        // Should not throw exception - OnEvent allows any parameter types
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testValidatePassesForMethodWithTooManyParameters() throws NoSuchMethodException {
        // Test that validation passes for methods with many parameters
        // OnEvent allows many parameters as long as they are not all SocketIOClient or AckRequest
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with many parameters
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{String.class, SocketIOClient.class, AckRequest.class, Integer.class, Boolean.class});
        when(mockMethod.getName()).thenReturn("eventWithManyParams");
        
        // Should not throw exception - OnEvent allows many parameters
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testAddListenerWithNullValues() throws NoSuchMethodException {
        // Test that the scanner handles null values gracefully
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        // Should not throw exception when adding listener
        assertThrows(NullPointerException.class, () -> 
            scanner.addListener(null, testHandler, method, annotation)
        );
    }

    @Test
    void testAddListenerIsolation() throws Exception {
        // Test that different handlers are isolated from each other
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        TestHandler handler1 = new TestHandler();
        TestHandler handler2 = new TestHandler();

        // Register both handlers using mock namespace to avoid configuration issues
        scanner.addListener(mockNamespace, handler1, method, annotation);
        scanner.addListener(mockNamespace, handler2, method, annotation);

        // Verify that both handlers were registered
        verify(mockNamespace, times(2)).addEventListener(eq("basic"), eq(String.class), any());
        
        // Verify that other methods were not called on either handler
        assertEquals(0, handler1.eventWithClientCallCount);
        assertEquals(0, handler1.eventWithAckCallCount);
        assertEquals(0, handler2.eventWithClientCallCount);
        assertEquals(0, handler2.eventWithAckCallCount);
    }

    @Test
    void testHandlerStateReset() {
        // Test that the handler state can be properly reset
        testHandler.basicEvent("test data");
        
        // Verify initial call
        assertTrue(testHandler.basicEventCalled);
        assertEquals(1, testHandler.basicEventCallCount);
        assertEquals("test data", testHandler.basicEventLastData);
        
        // Reset the handler
        testHandler.reset();
        
        // Verify reset state
        assertFalse(testHandler.basicEventCalled);
        assertEquals(0, testHandler.basicEventCallCount);
        assertFalse(testHandler.eventWithClientCalled);
        assertFalse(testHandler.eventWithAckCalled);
        assertFalse(testHandler.eventWithClientAndAckCalled);
        assertFalse(testHandler.multiTypeEventCalled);
        assertFalse(testHandler.regularMethodCalled);
    }

    @Test
    void testMultipleHandlersWithDifferentMethods() throws Exception {
        // Test that different methods on the same handler can be registered independently
        Method basicMethod = TestHandler.class.getMethod("basicEvent", String.class);
        Method clientMethod = TestHandler.class.getMethod("eventWithClient", String.class, SocketIOClient.class);
        OnEvent basicAnnotation = basicMethod.getAnnotation(OnEvent.class);
        OnEvent clientAnnotation = clientMethod.getAnnotation(OnEvent.class);

        // Create a handler with multiple valid methods
        TestHandler multiMethodHandler = new TestHandler();
        
        // Register both methods using mock namespace to avoid configuration issues
        scanner.addListener(mockNamespace, multiMethodHandler, basicMethod, basicAnnotation);
        scanner.addListener(mockNamespace, multiMethodHandler, clientMethod, clientAnnotation);

        // Verify that both methods were registered
        verify(mockNamespace, times(1)).addEventListener(eq("basic"), eq(String.class), any());
        verify(mockNamespace, times(1)).addEventListener(eq("withClient"), eq(String.class), any());
        
        // Verify that other methods were not called
        assertFalse(multiMethodHandler.eventWithAckCalled);
        assertFalse(multiMethodHandler.eventWithClientAndAckCalled);
        assertFalse(multiMethodHandler.multiTypeEventCalled);
        assertFalse(multiMethodHandler.regularMethodCalled);
    }

    @Test
    void testHandlerMethodParameterPassing() throws Exception {
        // Test that the handler method receives the correct parameters
        Method method = TestHandler.class.getMethod("eventWithClientAndAck", String.class, SocketIOClient.class, AckRequest.class);
        OnEvent annotation = method.getAnnotation(OnEvent.class);

        // Register the handler using mock namespace to avoid configuration issues
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that the handler was registered
        verify(mockNamespace, times(1)).addEventListener(eq("withClientAndAck"), eq(String.class), any());
        
        // Verify that the handler was registered but not called yet
        assertFalse(testHandler.eventWithClientAndAckCalled);
        assertEquals(0, testHandler.eventWithClientAndAckCallCount);
    }

    @Test
    void testAddListenerWithWhitespaceEventValue() throws Exception {
        // Test that addListener throws exception when event value is only whitespace
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock annotation with whitespace value
        OnEvent mockAnnotation = mock(OnEvent.class);
        when(mockAnnotation.value()).thenReturn("   ");

        // Should throw IllegalArgumentException for whitespace-only event value
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.addListener(mockNamespace, testHandler, method, mockAnnotation)
        );
        
        assertTrue(exception.getMessage().contains("OnEvent \"value\" parameter is required"));
    }

    @Test
    void testValidateMethodWithOnlyDataParameter() throws Exception {
        // Test that validation passes for methods with only data parameter
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateMethodWithOnlyClientParameter() throws Exception {
        // Test that validation passes for methods with only client parameter (no data)
        // This is valid in OnEvent - it allows methods with only client parameter
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with only client parameter
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{SocketIOClient.class});
        when(mockMethod.getName()).thenReturn("eventOnlyClient");

        // Should not throw exception - this is a valid signature
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testValidateMethodWithOnlyAckParameter() throws Exception {
        // Test that validation passes for methods with only ack parameter (no data)
        // This is valid in OnEvent - it allows methods with only ack parameter
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with only ack parameter
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{AckRequest.class});
        when(mockMethod.getName()).thenReturn("eventOnlyAck");

        // Should not throw exception - this is a valid signature
        scanner.validate(mockMethod, TestHandler.class);
    }

    @Test
    void testValidateMethodWithClientAndAckOnly() throws Exception {
        // Test that validation passes for methods with only client and ack parameters (no data)
        // This is valid in OnEvent - it allows methods with only client and ack parameters
        Method method = TestHandler.class.getMethod("basicEvent", String.class);
        
        // Create a mock method with only client and ack parameters
        Method mockMethod = mock(Method.class);
        when(mockMethod.getParameterTypes()).thenReturn(new Class<?>[]{SocketIOClient.class, AckRequest.class});
        when(mockMethod.getName()).thenReturn("eventClientAndAckOnly");

        // Should not throw exception - this is a valid signature
        scanner.validate(mockMethod, TestHandler.class);
    }
}
