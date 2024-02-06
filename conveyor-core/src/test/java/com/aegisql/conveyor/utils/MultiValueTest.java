package com.aegisql.conveyor.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

}