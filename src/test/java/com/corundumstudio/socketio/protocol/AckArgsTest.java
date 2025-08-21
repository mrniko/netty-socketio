package com.corundumstudio.socketio.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Comprehensive test suite for AckArgs class */
public class AckArgsTest extends BaseProtocolTest {

  @Test
  public void testConstructorWithValidArgs() {
    List<Object> args = Arrays.asList("arg1", "arg2", 123);
    AckArgs ackArgs = new AckArgs(args);

    assertEquals(args, ackArgs.getArgs());
    assertSame(args, ackArgs.getArgs());
  }

  @Test
  public void testConstructorWithEmptyArgs() {
    List<Object> emptyArgs = Collections.emptyList();
    AckArgs ackArgs = new AckArgs(emptyArgs);

    assertEquals(emptyArgs, ackArgs.getArgs());
    assertTrue(ackArgs.getArgs().isEmpty());
  }

  @Test
  public void testConstructorWithNullArgs() {
    AckArgs ackArgs = new AckArgs(null);

    assertNull(ackArgs.getArgs());
  }

  @Test
  public void testConstructorWithSingleArg() {
    List<Object> singleArg = Arrays.asList("single");
    AckArgs ackArgs = new AckArgs(singleArg);

    assertEquals(singleArg, ackArgs.getArgs());
    assertEquals(1, ackArgs.getArgs().size());
    assertEquals("single", ackArgs.getArgs().get(0));
  }

  @Test
  public void testConstructorWithMultipleArgs() {
    List<Object> multipleArgs = Arrays.asList("string", 42, 3.14, true, null);
    AckArgs ackArgs = new AckArgs(multipleArgs);

    assertEquals(multipleArgs, ackArgs.getArgs());
    assertEquals(5, ackArgs.getArgs().size());
    assertEquals("string", ackArgs.getArgs().get(0));
    assertEquals(42, ackArgs.getArgs().get(1));
    assertEquals(3.14, ackArgs.getArgs().get(2));
    assertEquals(true, ackArgs.getArgs().get(3));
    assertNull(ackArgs.getArgs().get(4));
  }

  @Test
  public void testConstructorWithComplexArgs() {
    List<Object> complexArgs =
        Arrays.asList(
            "string",
            123,
            456.78,
            true,
            Arrays.asList("nested", "list"),
            new Object() {
              @Override
              public String toString() {
                return "custom";
              }
            });

    AckArgs ackArgs = new AckArgs(complexArgs);

    assertEquals(complexArgs, ackArgs.getArgs());
    assertEquals(6, ackArgs.getArgs().size());
  }

  @Test
  public void testConstructorWithDifferentDataTypes() {
    List<Object> mixedArgs =
        Arrays.asList(
            "string",
            42,
            3.14,
            true,
            false,
            (byte) 127,
            (short) 32767,
            (long) 9223372036854775807L,
            (float) 2.718f,
            (double) 1.618);

    AckArgs ackArgs = new AckArgs(mixedArgs);

    assertEquals(mixedArgs, ackArgs.getArgs());
    assertEquals(10, ackArgs.getArgs().size());
  }

  @Test
  public void testGetArgsReturnsSameReference() {
    List<Object> originalArgs = Arrays.asList("arg1", "arg2");
    AckArgs ackArgs = new AckArgs(originalArgs);

    List<Object> returnedArgs = ackArgs.getArgs();
    assertSame(originalArgs, returnedArgs);
  }

  @Test
  public void testArgsImmutability() {
    List<Object> originalArgs = new ArrayList<>(Arrays.asList("original", "args"));
    AckArgs ackArgs = new AckArgs(originalArgs);

    // Verify original values
    assertEquals(originalArgs, ackArgs.getArgs());

    // Modify the original collection
    originalArgs.add("modified");

    // AckArgs should reflect the changes since it holds a direct reference
    // This is the actual behavior of the AckArgs class
    assertEquals(Arrays.asList("original", "args", "modified"), ackArgs.getArgs());
    assertEquals(3, ackArgs.getArgs().size());
  }

  @Test
  public void testAckArgsEquality() {
    List<Object> args1 = Arrays.asList("arg1", "arg2");
    List<Object> args2 = Arrays.asList("arg1", "arg2");

    AckArgs ackArgs1 = new AckArgs(args1);
    AckArgs ackArgs2 = new AckArgs(args2);

    // Test equality based on content
    assertEquals(ackArgs1.getArgs(), ackArgs2.getArgs());
  }

  @Test
  public void testAckArgsWithSpecialCharacters() {
    List<Object> argsWithSpecialChars = Arrays.asList("arg!@#", "arg$%^", "arg&*()");
    AckArgs ackArgs = new AckArgs(argsWithSpecialChars);

    assertEquals(argsWithSpecialChars, ackArgs.getArgs());
    assertEquals(3, ackArgs.getArgs().size());
  }

  @Test
  public void testAckArgsWithUnicodeCharacters() {
    List<Object> argsWithUnicode = Arrays.asList("参数1", "参数2", "参数3");
    AckArgs ackArgs = new AckArgs(argsWithUnicode);

    assertEquals(argsWithUnicode, ackArgs.getArgs());
    assertEquals(3, ackArgs.getArgs().size());
  }

  @Test
  public void testAckArgsWithLargeList() {
    List<Object> largeArgs = Arrays.asList(new Object[1000]);
    for (int i = 0; i < largeArgs.size(); i++) {
      largeArgs.set(i, "arg" + i);
    }

    AckArgs ackArgs = new AckArgs(largeArgs);

    assertEquals(largeArgs, ackArgs.getArgs());
    assertEquals(1000, ackArgs.getArgs().size());
    assertEquals("arg0", ackArgs.getArgs().get(0));
    assertEquals("arg999", ackArgs.getArgs().get(999));
  }

  @Test
  public void testAckArgsWithNestedCollections() {
    List<Object> nestedArgs =
        Arrays.asList(
            Arrays.asList("nested1", "nested2"),
            Arrays.asList(1, 2, 3),
            Collections.singletonMap("key", "value"));

    AckArgs ackArgs = new AckArgs(nestedArgs);

    assertEquals(nestedArgs, ackArgs.getArgs());
    assertEquals(3, ackArgs.getArgs().size());

    @SuppressWarnings("unchecked")
    List<Object> firstNested = (List<Object>) ackArgs.getArgs().get(0);
    assertEquals(Arrays.asList("nested1", "nested2"), firstNested);
  }
}
