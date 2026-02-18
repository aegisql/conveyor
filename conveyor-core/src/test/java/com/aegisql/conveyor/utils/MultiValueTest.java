package com.aegisql.conveyor.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiValueTest {

    @Test
    public void test() {
        MultiValue mv = new MultiValue("first").add("second");
        String first = mv.cast(String.class,0);
        assertNotNull(first);
        assertEquals("first",first);

        String second = mv.asString(1);
        assertNotNull(second);
        assertEquals("second",second);

        System.out.println(mv);
    }

    @Test
    public void testTypedAccessorsAndArray() {
        MultiValue mv = new MultiValue(10)
                .add(20L)
                .add(3.5d)
                .add(true)
                .add("tail");

        assertEquals(10, mv.asInteger(0));
        assertEquals(20L, mv.asLong(1));
        assertEquals(3.5d, mv.asDouble(2));
        assertEquals(true, mv.asBoolean(3));
        assertEquals("tail", mv.asString(4));
        assertEquals("tail", mv.asObject(4));

        Object[] arr = mv.asArray();
        assertEquals(5, arr.length);
        assertEquals(List.of(10, 20L, 3.5d, true, "tail"), mv.getValues());
        assertTrue(mv.toString().contains("MultiValue"));
    }

    @Test
    public void testValuesListIsImmutable() {
        MultiValue mv = new MultiValue("value");
        assertThrows(UnsupportedOperationException.class, () -> mv.getValues().add("another"));
    }

}
