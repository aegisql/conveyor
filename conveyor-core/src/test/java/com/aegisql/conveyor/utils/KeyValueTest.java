package com.aegisql.conveyor.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeyValueTest {

    @Test
    public void basicTest() {
        var a100 = new KeyValue<String,Integer>("A",100);
        var a200 = a100.value(200);
        var b200 = a200.key("B");
        assertEquals("A",a100.key);
        assertEquals("A",a200.key);
        assertEquals("B",b200.key);
        assertEquals(Integer.valueOf(100),a100.value);
        assertEquals(Integer.valueOf(200),a200.value);
        assertEquals(Integer.valueOf(200),b200.value);

        assertEquals(a100,new KeyValue<String,Integer>("A",100));
        System.out.println(a100);
    }

}