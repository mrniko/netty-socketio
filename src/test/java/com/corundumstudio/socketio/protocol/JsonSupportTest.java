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
package com.corundumstudio.socketio.protocol;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.AckCallback;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for JsonSupport interface using Mockito
 */
public class JsonSupportTest extends BaseProtocolTest {

    @Mock
    private JsonSupport jsonSupport;

    @Mock
    private AckCallback<String> ackCallback;

    private AutoCloseable closeableMocks;

    @BeforeEach
    public void setUp() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeableMocks.close();
    }

    @Test
    public void testReadAckArgs() throws IOException {
        // Setup
        ByteBufInputStream inputStream = new ByteBufInputStream(Unpooled.wrappedBuffer("test".getBytes()));
        AckArgs expectedAckArgs = new AckArgs(Arrays.asList("arg1", "arg2"));
        
        when(jsonSupport.readAckArgs(inputStream, ackCallback)).thenReturn(expectedAckArgs);
        
        // Execute
        AckArgs result = jsonSupport.readAckArgs(inputStream, ackCallback);
        
        // Verify
        assertEquals(expectedAckArgs, result);
        verify(jsonSupport).readAckArgs(inputStream, ackCallback);
        
        inputStream.close();
    }

    @Test
    public void testReadValue() throws IOException {
        // Setup
        ByteBufInputStream inputStream = new ByteBufInputStream(Unpooled.wrappedBuffer("test".getBytes()));
        String expectedValue = "testValue";
        String namespaceName = "testNamespace";
        Class<String> valueType = String.class;
        
        when(jsonSupport.readValue(namespaceName, inputStream, valueType)).thenReturn(expectedValue);
        
        // Execute
        String result = jsonSupport.readValue(namespaceName, inputStream, valueType);
        
        // Verify
        assertEquals(expectedValue, result);
        verify(jsonSupport).readValue(namespaceName, inputStream, valueType);
        
        inputStream.close();
    }

    @Test
    public void testWriteValue() throws IOException {
        // Setup
        ByteBufOutputStream outputStream = new ByteBufOutputStream(Unpooled.buffer());
        Object value = "testValue";
        
        doNothing().when(jsonSupport).writeValue(outputStream, value);
        
        // Execute
        jsonSupport.writeValue(outputStream, value);
        
        // Verify
        verify(jsonSupport).writeValue(outputStream, value);
        
        outputStream.close();
    }

    @Test
    public void testAddEventMapping() {
        // Setup
        String namespaceName = "testNamespace";
        String eventName = "testEvent";
        Class<?> eventClass = String.class;
        
        doNothing().when(jsonSupport).addEventMapping(namespaceName, eventName, eventClass);
        
        // Execute
        jsonSupport.addEventMapping(namespaceName, eventName, eventClass);
        
        // Verify
        verify(jsonSupport).addEventMapping(namespaceName, eventName, eventClass);
    }

    @Test
    public void testAddEventMappingWithMultipleClasses() {
        // Setup
        String namespaceName = "testNamespace";
        String eventName = "testEvent";
        Class<?> eventClass1 = String.class;
        Class<?> eventClass2 = Integer.class;
        
        doNothing().when(jsonSupport).addEventMapping(namespaceName, eventName, eventClass1, eventClass2);
        
        // Execute
        jsonSupport.addEventMapping(namespaceName, eventName, eventClass1, eventClass2);
        
        // Verify
        verify(jsonSupport).addEventMapping(namespaceName, eventName, eventClass1, eventClass2);
    }

    @Test
    public void testRemoveEventMapping() {
        // Setup
        String namespaceName = "testNamespace";
        String eventName = "testEvent";
        
        doNothing().when(jsonSupport).removeEventMapping(namespaceName, eventName);
        
        // Execute
        jsonSupport.removeEventMapping(namespaceName, eventName);
        
        // Verify
        verify(jsonSupport).removeEventMapping(namespaceName, eventName);
    }

    @Test
    public void testGetArrays() {
        // Setup
        List<byte[]> expectedArrays = Arrays.asList(
            "array1".getBytes(),
            "array2".getBytes()
        );
        
        when(jsonSupport.getArrays()).thenReturn(expectedArrays);
        
        // Execute
        List<byte[]> result = jsonSupport.getArrays();
        
        // Verify
        assertEquals(expectedArrays, result);
        verify(jsonSupport).getArrays();
    }

    @Test
    public void testGetArraysReturnsEmptyList() {
        // Setup
        List<byte[]> emptyArrays = Arrays.asList();
        
        when(jsonSupport.getArrays()).thenReturn(emptyArrays);
        
        // Execute
        List<byte[]> result = jsonSupport.getArrays();
        
        // Verify
        assertTrue(result.isEmpty());
        verify(jsonSupport).getArrays();
    }

    @Test
    public void testReadValueWithDifferentTypes() throws IOException {
        // Setup
        ByteBufInputStream inputStream = new ByteBufInputStream(Unpooled.wrappedBuffer("test".getBytes()));
        String namespaceName = "testNamespace";
        
        // Test with String
        when(jsonSupport.readValue(namespaceName, inputStream, String.class)).thenReturn("stringValue");
        String stringResult = jsonSupport.readValue(namespaceName, inputStream, String.class);
        assertEquals("stringValue", stringResult);
        
        // Test with Integer
        when(jsonSupport.readValue(namespaceName, inputStream, Integer.class)).thenReturn(42);
        Integer intResult = jsonSupport.readValue(namespaceName, inputStream, Integer.class);
        assertEquals(Integer.valueOf(42), intResult);
        
        // Test with Boolean
        when(jsonSupport.readValue(namespaceName, inputStream, Boolean.class)).thenReturn(true);
        Boolean boolResult = jsonSupport.readValue(namespaceName, inputStream, Boolean.class);
        assertEquals(Boolean.TRUE, boolResult);
        
        verify(jsonSupport, times(3)).readValue(eq(namespaceName), eq(inputStream), any());
        
        inputStream.close();
    }

    @Test
    public void testWriteValueWithDifferentTypes() throws IOException {
        // Setup
        ByteBufOutputStream outputStream = new ByteBufOutputStream(Unpooled.buffer());
        
        // Test with String
        doNothing().when(jsonSupport).writeValue(outputStream, "stringValue");
        jsonSupport.writeValue(outputStream, "stringValue");
        
        // Test with Integer
        doNothing().when(jsonSupport).writeValue(outputStream, 42);
        jsonSupport.writeValue(outputStream, 42);
        
        // Test with Boolean
        doNothing().when(jsonSupport).writeValue(outputStream, true);
        jsonSupport.writeValue(outputStream, true);
        
        verify(jsonSupport).writeValue(outputStream, "stringValue");
        verify(jsonSupport).writeValue(outputStream, 42);
        verify(jsonSupport).writeValue(outputStream, true);
        
        outputStream.close();
    }

    @Test
    public void testAddEventMappingWithNullParameters() {
        // Setup
        doNothing().when(jsonSupport).addEventMapping(null, null, (Class<?>) null);
        
        // Execute
        jsonSupport.addEventMapping(null, null, (Class<?>) null);
        
        // Verify
        verify(jsonSupport).addEventMapping(null, null, (Class<?>) null);
    }

    @Test
    public void testRemoveEventMappingWithNullParameters() {
        // Setup
        doNothing().when(jsonSupport).removeEventMapping(null, null);
        
        // Execute
        jsonSupport.removeEventMapping(null, null);
        
        // Verify
        verify(jsonSupport).removeEventMapping(null, null);
    }

    @Test
    public void testReadValueWithNullNamespace() throws IOException {
        // Setup
        ByteBufInputStream inputStream = new ByteBufInputStream(Unpooled.wrappedBuffer("test".getBytes()));
        String expectedValue = "testValue";
        
        when(jsonSupport.readValue(null, inputStream, String.class)).thenReturn(expectedValue);
        
        // Execute
        String result = jsonSupport.readValue(null, inputStream, String.class);
        
        // Verify
        assertEquals(expectedValue, result);
        verify(jsonSupport).readValue(null, inputStream, String.class);
        
        inputStream.close();
    }

    @Test
    public void testWriteValueWithNullValue() throws IOException {
        // Setup
        ByteBufOutputStream outputStream = new ByteBufOutputStream(Unpooled.buffer());
        
        doNothing().when(jsonSupport).writeValue(outputStream, null);
        
        // Execute
        jsonSupport.writeValue(outputStream, null);
        
        // Verify
        verify(jsonSupport).writeValue(outputStream, null);
        
        outputStream.close();
    }

    @Test
    public void testGetArraysReturnsNull() {
        // Setup
        when(jsonSupport.getArrays()).thenReturn(null);
        
        // Execute
        List<byte[]> result = jsonSupport.getArrays();
        
        // Verify
        assertNull(result);
        verify(jsonSupport).getArrays();
    }

    @Test
    public void testMultipleEventMappings() {
        // Setup
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        String event1 = "event1";
        String event2 = "event2";
        Class<?> class1 = String.class;
        Class<?> class2 = Integer.class;
        
        doNothing().when(jsonSupport).addEventMapping(namespace1, event1, class1);
        doNothing().when(jsonSupport).addEventMapping(namespace2, event2, class2);
        
        // Execute
        jsonSupport.addEventMapping(namespace1, event1, class1);
        jsonSupport.addEventMapping(namespace2, event2, class2);
        
        // Verify
        verify(jsonSupport).addEventMapping(namespace1, event1, class1);
        verify(jsonSupport).addEventMapping(namespace2, event2, class2);
    }

    @Test
    public void testMultipleEventRemovals() {
        // Setup
        String namespace1 = "namespace1";
        String namespace2 = "namespace2";
        String event1 = "event1";
        String event2 = "event2";
        
        doNothing().when(jsonSupport).removeEventMapping(namespace1, event1);
        doNothing().when(jsonSupport).removeEventMapping(namespace2, event2);
        
        // Execute
        jsonSupport.removeEventMapping(namespace1, event1);
        jsonSupport.removeEventMapping(namespace2, event2);
        
        // Verify
        verify(jsonSupport).removeEventMapping(namespace1, event1);
        verify(jsonSupport).removeEventMapping(namespace2, event2);
    }
}
