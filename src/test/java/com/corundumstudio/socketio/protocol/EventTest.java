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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive test suite for Event class
 */
public class EventTest extends BaseProtocolTest {

    @Test
    public void testDefaultConstructor() {
        Event event = new Event();
        
        assertNull(event.getName());
        assertNull(event.getArgs());
    }

    @Test
    public void testParameterizedConstructor() {
        String eventName = "testEvent";
        List<Object> args = Arrays.asList("arg1", "arg2", 123);
        
        Event event = new Event(eventName, args);
        
        assertEquals(eventName, event.getName());
        assertEquals(args, event.getArgs());
        assertSame(args, event.getArgs());
    }

    @Test
    public void testParameterizedConstructorWithEmptyArgs() {
        String eventName = "emptyEvent";
        List<Object> emptyArgs = Collections.emptyList();
        
        Event event = new Event(eventName, emptyArgs);
        
        assertEquals(eventName, event.getName());
        assertEquals(emptyArgs, event.getArgs());
        assertTrue(event.getArgs().isEmpty());
    }

    @Test
    public void testParameterizedConstructorWithNullArgs() {
        String eventName = "nullArgsEvent";
        
        Event event = new Event(eventName, null);
        
        assertEquals(eventName, event.getName());
        assertNull(event.getArgs());
    }

    @Test
    public void testParameterizedConstructorWithComplexArgs() {
        String eventName = "complexEvent";
        List<Object> complexArgs = Arrays.asList(
            "string",
            123,
            456.78,
            true,
            null,
            Arrays.asList("nested", "list"),
            new Object() { @Override public String toString() { return "custom"; } }
        );
        
        Event event = new Event(eventName, complexArgs);
        
        assertEquals(eventName, event.getName());
        assertEquals(complexArgs, event.getArgs());
        assertEquals(7, event.getArgs().size());
    }

    @Test
    public void testGetNameAndArgs() {
        // Test getting name and args from constructed events
        Event event1 = new Event("event1", Arrays.asList("arg1", "arg2"));
        assertEquals("event1", event1.getName());
        assertEquals(Arrays.asList("arg1", "arg2"), event1.getArgs());
        
        Event event2 = new Event("event2", Arrays.asList(1, 2, 3));
        assertEquals("event2", event2.getName());
        assertEquals(Arrays.asList(1, 2, 3), event2.getArgs());
    }

    @Test
    public void testEventWithDifferentDataTypes() {
        // Test with different data types
        List<Object> mixedArgs = Arrays.asList(
            "string",
            42,
            3.14,
            true,
            false,
            (byte) 127,
            (short) 32767,
            (long) 9223372036854775807L,
            (float) 2.718f,
            (double) 1.618
        );
        
        Event event = new Event("mixedTypesEvent", mixedArgs);
        
        assertEquals("mixedTypesEvent", event.getName());
        assertEquals(mixedArgs, event.getArgs());
        assertEquals(10, event.getArgs().size());
    }

    @Test
    public void testEventImmutability() {
        String originalName = "originalName";
        List<Object> originalArgs = new ArrayList<>(Arrays.asList("original", "args"));
        
        Event event = new Event(originalName, originalArgs);
        
        // Verify original values
        assertEquals(originalName, event.getName());
        assertEquals(originalArgs, event.getArgs());
        
        // Modify the original list (name is String, so it's immutable)
        originalArgs.add("modified");
        
        // Event should reflect the changes in args since it holds a direct reference
        // This is the actual behavior of the Event class
        assertEquals(Arrays.asList("original", "args", "modified"), event.getArgs());
        assertEquals(3, event.getArgs().size());
        
        // Name should remain unchanged since String is immutable
        assertEquals("originalName", event.getName());
    }

    @Test
    public void testEventEquality() {
        Event event1 = new Event("sameEvent", Arrays.asList("arg1", "arg2"));
        Event event2 = new Event("sameEvent", Arrays.asList("arg1", "arg2"));
        Event event3 = new Event("differentEvent", Arrays.asList("arg1", "arg2"));
        Event event4 = new Event("sameEvent", Arrays.asList("different", "args"));
        
        // Test equality based on content
        assertEquals(event1.getName(), event2.getName());
        assertEquals(event1.getArgs(), event2.getArgs());
        
        // Test inequality
        assertNotEquals(event1.getName(), event3.getName());
        assertNotEquals(event1.getArgs(), event4.getArgs());
    }

    @Test
    public void testEventWithSpecialCharacters() {
        String eventNameWithSpecialChars = "event!@#$%^&*()_+-=[]{}|;':\",./<>?";
        List<Object> argsWithSpecialChars = Arrays.asList("arg!@#", "arg$%^", "arg&*()");
        
        Event event = new Event(eventNameWithSpecialChars, argsWithSpecialChars);
        
        assertEquals(eventNameWithSpecialChars, event.getName());
        assertEquals(argsWithSpecialChars, event.getArgs());
    }

    @Test
    public void testEventWithUnicodeCharacters() {
        String eventNameWithUnicode = "事件名称";
        List<Object> argsWithUnicode = Arrays.asList("参数1", "参数2", "参数3");
        
        Event event = new Event(eventNameWithUnicode, argsWithUnicode);
        
        assertEquals(eventNameWithUnicode, event.getName());
        assertEquals(argsWithUnicode, event.getArgs());
    }
}
