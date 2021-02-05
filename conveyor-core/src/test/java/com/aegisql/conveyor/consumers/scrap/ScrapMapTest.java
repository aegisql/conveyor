package com.aegisql.conveyor.consumers.scrap;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ScrapMapTest {

    ScrapMap<Integer> sm;
    @Before
    public void init() {
        sm = new ScrapMap<>();
    }

    @Test
    public void creationTests() {
        ScrapMap<Integer> sm1 = new ScrapMap<>(new ConcurrentHashMap<>());
        ScrapMap<Integer> sm2 = new ScrapMap<>(()->new ConcurrentHashMap());
        assertTrue(sm1.isEmpty());
        assertTrue(sm2.isEmpty());
        assertFalse(sm1.containsValue("X"));
        assertFalse(sm2.containsValue("X"));
        sm2.keySet();
        sm2.values();
        sm2.entrySet();
        assertNotNull(sm2.unwrap());
    }

    @Test
    public void ofTests() {
        assertNotNull(ScrapMap.of(null));
        assertNotNull(ScrapMap.of(null,new ConcurrentHashMap<>()));
        assertNotNull(ScrapMap.of(null,()->new ConcurrentHashMap()));
    }

    @Test(expected = RuntimeException.class)
    public void failPutTest() {
        sm.put(1, Arrays.asList("X"));
    }

    @Test(expected = RuntimeException.class)
    public void failPutAllTest() {
        sm.putAll(new HashMap<>());
    }

    @Test(expected = RuntimeException.class)
    public void failRemoveTest() {
        sm.remove(1);
    }

    @Test(expected = RuntimeException.class)
    public void failClearTest() {
        sm.clear();
    }

    @Test(expected = RuntimeException.class)
    public void failPutAbsentrTest() {
        sm.putIfAbsent(null,null);
    }

    @Test(expected = RuntimeException.class)
    public void failRemove2Test() {
        sm.remove(1,1);
    }

    @Test(expected = RuntimeException.class)
    public void failReplaceTest() {
        sm.replace(1,null);
    }

    @Test(expected = RuntimeException.class)
    public void failReplace2Test() {
        sm.replace(1,null,null);
    }


}