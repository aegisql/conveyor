package com.aegisql.conveyor.consumers.scrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ScrapMapTest {

    ScrapMap<Integer> sm;
    @BeforeEach
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

    @Test
    public void failPutTest() {
        assertThrows(RuntimeException.class,()->sm.put(1, Arrays.asList("X")));
    }

    @Test
    public void failPutAllTest() {
        assertThrows(RuntimeException.class,()->sm.putAll(new HashMap<>()));
    }

    @Test
    public void failRemoveTest() {
        assertThrows(RuntimeException.class,()->sm.remove(1));
    }

    @Test
    public void failClearTest() {
        assertThrows(RuntimeException.class,()->sm.clear());
    }

    @Test
    public void failPutAbsentrTest() {
        assertThrows(RuntimeException.class,()->sm.putIfAbsent(null,null));
    }

    @Test
    public void failRemove2Test() {
        assertThrows(RuntimeException.class,()->sm.remove(1,1));
    }

    @Test
    public void failReplaceTest() {
        assertThrows(RuntimeException.class,()->sm.replace(1,null));
    }

    @Test
    public void failReplace2Test() {
        assertThrows(RuntimeException.class,()->sm.replace(1,null,null));
    }


}