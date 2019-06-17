package com.aegisql.conveyor;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LookupPerformanceTest {

    private static int CONVEYORS = 10;
    private static int TESTS = 10_000_000;


    @Test
    public void hashTablePerfTest() {
        Map<String, Conveyor<Integer, String, String>> conveyors = createConveyors("FOR_HASH_TABLE_");

        long start = System.currentTimeMillis();
        for(int i = 0; i < TESTS; i++) {
            for(String name: conveyors.keySet()) {
                getType(name);
                if(conveyors.containsKey(name)) {
                    Conveyor<Integer, String, String> c = conveyors.get(name);
                    assertNotNull(c);
                    assertTrue(c.isRunning());
                } else {
                    fail("Expected conveyors!");
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("hashTablePerfTest "+(end-start)/1000.0+"s");
    }

    @Test
    public void jmxLookupPerfTest() {
        Map<String, Conveyor<Integer, String, String>> conveyors = createConveyors("FOR_JMX_SERVER_");

        long start = System.currentTimeMillis();
        for(int i = 0; i < TESTS; i++) {
            for(String name: conveyors.keySet()) {
                Conveyor c = Conveyor.byName(name);
                assertNotNull(c);
                assertTrue(c.isRunning());
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("jmxLookupPerfTest "+(end-start)/1000.0+"s");
    }

    private Map<String,Conveyor<Integer,String,String>> createConveyors(String namePrefix) {
        Map<String, Conveyor<Integer, String, String>> conveyors = new HashMap<>();

        for(int i = 0; i < CONVEYORS; i++) {
            AssemblingConveyor<Integer,String,String> c = new AssemblingConveyor<>();
            c.setName(namePrefix+(i+1));
            conveyors.put(c.getName(),c);
            assertNotNull(Conveyor.byName(c.getName()));
        }

        return conveyors;
    }


    private String getType(String name) {
        return "com.aegisql.conveyor:type="+name;
    }

}
