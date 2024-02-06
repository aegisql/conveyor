package com.aegisql.conveyor.consumers.result;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ResultMapTest {

    @Test
    public void resultMapTest() {
        ResultMap<Integer,String> rm1 = new ResultMap<>();
        ResultMap<Integer,String> rm2 = new ResultMap<>(new HashMap<>());
        ResultMap<Integer,String> rm3 = new ResultMap<>(HashMap::new);
        assertTrue(rm1.isEmpty());
        assertFalse(rm1.containsValue("test"));
        assertFalse(rm1.containsKey(1));
        assertNotNull(rm1.unwrap());

        rm1.accept(ResultConsumerTest.getProductBin(1,"test"));
        assertNotNull(rm1.keySet());
        assertNotNull(rm1.values());
        assertNotNull(rm1.entrySet());


    }

    @Test
    public void ofTest() {
        ResultMap<Integer,String> rm1 = ResultMap.of(null);
        ResultMap<Integer,String> rm2 = ResultMap.of(null,new HashMap<>());
        ResultMap<Integer,String> rm3 = ResultMap.of(null,HashMap::new);
        assertNotNull(rm1);
        assertNotNull(rm2);
        assertNotNull(rm3);
    }

    @Test
    public void putTest() {
        ResultMap<Integer,String> rm1 = new ResultMap<>();
        assertThrows(RuntimeException.class,()->rm1.put(1,"test"));
    }
    @Test
    public void putAllTest() {
        ResultMap<Integer,String> rm1 = new ResultMap<>();
        assertThrows(RuntimeException.class,()->rm1.putAll(new HashMap<>()));
    }
    @Test
    public void removeTest() {
        ResultMap<Integer,String> rm1 = new ResultMap<>();
        assertThrows(RuntimeException.class,()->rm1.remove(1));
    }
    @Test
    public void clearTest() {
        ResultMap<Integer,String> rm1 = new ResultMap<>();
        assertThrows(RuntimeException.class,()->rm1.clear());
    }
}