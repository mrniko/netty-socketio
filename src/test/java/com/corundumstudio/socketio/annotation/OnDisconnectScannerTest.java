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
 * Unit tests for OnDisconnectScanner class.
 * Tests the functionality of scanning and registering OnDisconnect annotation handlers.
 */
class OnDisconnectScannerTest extends AnnotationTestBase {

    private OnDisconnectScanner scanner;
    private Configuration config;
    private Namespace realNamespace;

    @Mock
    private Namespace mockNamespace;

    @Mock
    private SocketIOClient mockClient;

    private TestHandler testHandler;

    /**
     * Test handler class with OnDisconnect annotated methods.
     * Used to verify that the scanner correctly registers and invokes methods.
     * Each method tracks its own call statistics for precise validation.
     */
    public static class TestHandler {
        // OnDisconnect method tracking
        public boolean onDisconnectCalled = false;
        public SocketIOClient onDisconnectLastClient = null;
        public int onDisconnectCallCount = 0;

        // Invalid param method tracking
        public boolean onDisconnectInvalidParamCalled = false;
        public String onDisconnectInvalidParamLastParam = null;
        public int onDisconnectInvalidParamCallCount = 0;

        // Wrong param count method tracking
        public boolean onDisconnectWrongParamCountCalled = false;
        public SocketIOClient onDisconnectWrongParamCountLastClient = null;
        public String onDisconnectWrongParamCountLastExtra = null;
        public int onDisconnectWrongParamCountCallCount = 0;

        // Regular method tracking
        public boolean regularMethodCalled = false;
        public SocketIOClient regularMethodLastClient = null;
        public int regularMethodCallCount = 0;

        /**
         * Valid OnDisconnect method with correct signature.
         * Should be successfully registered and invoked.
         */
        @OnDisconnect
        public void onDisconnect(SocketIOClient client) {
            onDisconnectCalled = true;
            onDisconnectLastClient = client;
            onDisconnectCallCount++;
        }

        /**
         * Invalid OnDisconnect method with wrong parameter type.
         * Should cause validation to fail.
         */
        @OnDisconnect
        public void onDisconnectInvalidParam(String client) {
            onDisconnectInvalidParamCalled = true;
            onDisconnectInvalidParamLastParam = client;
            onDisconnectInvalidParamCallCount++;
        }

        /**
         * Invalid OnDisconnect method with wrong number of parameters.
         * Should cause validation to fail.
         */
        @OnDisconnect
        public void onDisconnectWrongParamCount(SocketIOClient client, String extra) {
            onDisconnectWrongParamCountCalled = true;
            onDisconnectWrongParamCountLastClient = client;
            onDisconnectWrongParamCountLastExtra = extra;
            onDisconnectWrongParamCountCallCount++;
        }

        /**
         * Method without OnDisconnect annotation.
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
            // Reset onDisconnect method state
            onDisconnectCalled = false;
            onDisconnectLastClient = null;
            onDisconnectCallCount = 0;

            // Reset invalid param method state
            onDisconnectInvalidParamCalled = false;
            onDisconnectInvalidParamLastParam = null;
            onDisconnectInvalidParamCallCount = 0;

            // Reset wrong param count method state
            onDisconnectWrongParamCountCalled = false;
            onDisconnectWrongParamCountLastClient = null;
            onDisconnectWrongParamCountLastExtra = null;
            onDisconnectWrongParamCountCallCount = 0;

            // Reset regular method state
            regularMethodCalled = false;
            regularMethodLastClient = null;
            regularMethodCallCount = 0;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scanner = new OnDisconnectScanner();
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
        assertEquals(OnDisconnect.class, annotationType);
    }

    @Test
    void testAddListenerSuccessfullyRegistersHandler() throws Exception {
        // Test that addListener correctly registers the handler with the namespace
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Execute the scanner
        scanner.addListener(mockNamespace, testHandler, method, annotation);

        // Verify that addDisconnectListener was called on the namespace
        verify(mockNamespace, times(1)).addDisconnectListener(any());
        
        // Verify that our test handler hasn't been called yet
        assertFalse(testHandler.onDisconnectCalled);
        assertEquals(0, testHandler.onDisconnectCallCount);
    }

    @Test
    void testAddListenerInvokesHandlerMethod() throws Exception {
        // Test that when a client disconnects, the registered handler method is actually invoked
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Register the handler using the scanner
        scanner.addListener(realNamespace, testHandler, method, annotation);

        // Verify initial state
        assertFalse(testHandler.onDisconnectCalled);
        assertEquals(0, testHandler.onDisconnectCallCount);

        // Simulate client disconnection by calling onDisconnect on the namespace
        realNamespace.onDisconnect(mockClient);

        // Verify that the handler method was actually called
        assertTrue(testHandler.onDisconnectCalled);
        assertEquals(mockClient, testHandler.onDisconnectLastClient);
        assertEquals(1, testHandler.onDisconnectCallCount);
        
        // Verify that other methods were not called
        assertFalse(testHandler.onDisconnectInvalidParamCalled);
        assertFalse(testHandler.onDisconnectWrongParamCountCalled);
        assertFalse(testHandler.regularMethodCalled);
    }

    @Test
    void testAddListenerHandlesMultipleDisconnections() throws Exception {
        // Test that the handler can handle multiple client disconnections
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Register the handler
        scanner.addListener(realNamespace, testHandler, method, annotation);

        // Simulate multiple client disconnections
        SocketIOClient client1 = mock(SocketIOClient.class);
        SocketIOClient client2 = mock(SocketIOClient.class);
        SocketIOClient client3 = mock(SocketIOClient.class);

        when(client1.getSessionId()).thenReturn(UUID.randomUUID());
        when(client2.getSessionId()).thenReturn(UUID.randomUUID());
        when(client3.getSessionId()).thenReturn(UUID.randomUUID());

        // Disconnect multiple clients
        realNamespace.onDisconnect(client1);
        realNamespace.onDisconnect(client2);
        realNamespace.onDisconnect(client3);

        // Verify the handler was called for each disconnection
        assertTrue(testHandler.onDisconnectCalled);
        assertEquals(3, testHandler.onDisconnectCallCount);
        assertEquals(client3, testHandler.onDisconnectLastClient); // Last client should be the most recent
        
        // Verify that other methods were not called
        assertEquals(0, testHandler.onDisconnectInvalidParamCallCount);
        assertEquals(0, testHandler.onDisconnectWrongParamCountCallCount);
        assertEquals(0, testHandler.regularMethodCallCount);
    }

    @Test
    void testValidateCorrectMethodSignature() throws Exception {
        // Test that validation passes for methods with correct signature
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        
        // Should not throw any exception
        scanner.validate(method, TestHandler.class);
    }

    @Test
    void testValidateWrongParameterType() throws NoSuchMethodException {
        // Test that validation fails for methods with wrong parameter type
        Method method = TestHandler.class.getMethod("onDisconnectInvalidParam", String.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnDisconnect listener signature"));
        assertTrue(exception.getMessage().contains("onDisconnectInvalidParam"));
    }

    @Test
    void testValidateWrongParameterCount() throws NoSuchMethodException {
        // Test that validation fails for methods with wrong number of parameters
        Method method = TestHandler.class.getMethod("onDisconnectWrongParamCount", SocketIOClient.class, String.class);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnDisconnect listener signature"));
        assertTrue(exception.getMessage().contains("onDisconnectWrongParamCount"));
    }

    @Test
    void testValidateNoParameters() throws NoSuchMethodException {
        // Test that validation fails for methods with no parameters
        Method method = TestHandler.class.getMethod("reset");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> scanner.validate(method, TestHandler.class)
        );
        
        assertTrue(exception.getMessage().contains("Wrong OnDisconnect listener signature"));
    }

    @Test
    void testAddListenerWithExceptionHandling() throws Exception {
        // Test that the scanner properly handles exceptions during method invocation
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Create a handler that throws an exception
        TestHandler exceptionHandler = new TestHandler() {
            @Override
            @OnDisconnect
            public void onDisconnect(SocketIOClient client) {
                super.onDisconnect(client); // Record the call
                throw new RuntimeException("Test exception");
            }
        };

        // Register the handler
        scanner.addListener(realNamespace, exceptionHandler, method, annotation);

        // Verify initial state
        assertFalse(exceptionHandler.onDisconnectCalled);

        // Simulate client disconnection - exceptions are caught by Namespace.onDisconnect
        // and passed to exceptionListener, so no exception should be thrown here
        realNamespace.onDisconnect(mockClient);
        
        // Verify that the handler method was called despite the exception
        assertTrue(exceptionHandler.onDisconnectCalled);
        assertEquals(mockClient, exceptionHandler.onDisconnectLastClient);
        assertEquals(1, exceptionHandler.onDisconnectCallCount);
        
        // The test passes if no exception is thrown, as the Namespace handles it
        // We can verify that the exception was logged by checking the logs if needed
    }

    @Test
    void testAddListenerIsolation() throws Exception {
        // Test that different handlers are isolated from each other
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        TestHandler handler1 = new TestHandler();
        TestHandler handler2 = new TestHandler();

        // Register both handlers
        scanner.addListener(realNamespace, handler1, method, annotation);
        scanner.addListener(realNamespace, handler2, method, annotation);

        // Verify initial state
        assertFalse(handler1.onDisconnectCalled);
        assertFalse(handler2.onDisconnectCalled);

        // Simulate client disconnection
        realNamespace.onDisconnect(mockClient);

        // Verify both handlers were called independently
        assertTrue(handler1.onDisconnectCalled);
        assertTrue(handler2.onDisconnectCalled);
        assertEquals(mockClient, handler1.onDisconnectLastClient);
        assertEquals(mockClient, handler2.onDisconnectLastClient);
        assertEquals(1, handler1.onDisconnectCallCount);
        assertEquals(1, handler2.onDisconnectCallCount);
        
        // Verify other methods were not called on either handler
        assertEquals(0, handler1.onDisconnectInvalidParamCallCount);
        assertEquals(0, handler1.regularMethodCallCount);
        assertEquals(0, handler2.onDisconnectInvalidParamCallCount);
        assertEquals(0, handler2.regularMethodCallCount);
    }

    @Test
    void testAddListenerWithNullValues() throws NoSuchMethodException {
        // Test that the scanner handles null values gracefully
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Should not throw exception when adding listener
        assertThrows(NullPointerException.class, () -> 
            scanner.addListener(null, testHandler, method, annotation)
        );
    }

    @Test
    void testHandlerStateReset() {
        // Test that the handler state can be properly reset
        testHandler.onDisconnect(mockClient);
        
        // Verify initial call
        assertTrue(testHandler.onDisconnectCalled);
        assertEquals(1, testHandler.onDisconnectCallCount);
        
        // Reset the handler
        testHandler.reset();
        
        // Verify reset state
        assertFalse(testHandler.onDisconnectCalled);
        assertEquals(0, testHandler.onDisconnectCallCount);
        assertFalse(testHandler.onDisconnectInvalidParamCalled);
        assertFalse(testHandler.onDisconnectWrongParamCountCalled);
        assertFalse(testHandler.regularMethodCalled);
    }

    @Test
    void testMultipleHandlersWithDifferentMethods() throws Exception {
        // Test that different methods on the same handler can be registered independently
        Method disconnectMethod = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect disconnectAnnotation = disconnectMethod.getAnnotation(OnDisconnect.class);

        // Create a handler with multiple valid methods
        TestHandler multiMethodHandler = new TestHandler();
        
        // Register the handler
        scanner.addListener(realNamespace, multiMethodHandler, disconnectMethod, disconnectAnnotation);

        // Simulate client disconnection
        realNamespace.onDisconnect(mockClient);

        // Verify the correct method was called
        assertTrue(multiMethodHandler.onDisconnectCalled);
        assertEquals(1, multiMethodHandler.onDisconnectCallCount);
        
        // Verify other methods were not called
        assertFalse(multiMethodHandler.onDisconnectInvalidParamCalled);
        assertFalse(multiMethodHandler.onDisconnectWrongParamCountCalled);
        assertFalse(multiMethodHandler.regularMethodCalled);
    }

    @Test
    void testHandlerMethodParameterPassing() throws Exception {
        // Test that the handler method receives the correct client parameter
        Method method = TestHandler.class.getMethod("onDisconnect", SocketIOClient.class);
        OnDisconnect annotation = method.getAnnotation(OnDisconnect.class);

        // Register the handler
        scanner.addListener(realNamespace, testHandler, method, annotation);

        // Create a specific client for testing
        SocketIOClient testClient = mock(SocketIOClient.class);
        UUID testSessionId = UUID.randomUUID();
        when(testClient.getSessionId()).thenReturn(testSessionId);

        // Simulate disconnection with the test client
        realNamespace.onDisconnect(testClient);

        // Verify the handler received the correct client
        assertTrue(testHandler.onDisconnectCalled);
        assertEquals(testClient, testHandler.onDisconnectLastClient);
        assertEquals(testSessionId, testHandler.onDisconnectLastClient.getSessionId());
        assertEquals(1, testHandler.onDisconnectCallCount);
    }
}
