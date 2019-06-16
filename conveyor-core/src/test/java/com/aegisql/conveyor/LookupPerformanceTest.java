package com.aegisql.conveyor;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class LookupPerformanceTest {

    private static int CONVEYORS = 10;
    private static int TESTS = 10_000_000;


    @Test
    public void hashTablePerfTest() {
        Map<String, Conveyor<Integer, String, String>> conveyors = createConveyors("FOR_HASH_TABLE_");

        long start = System.currentTimeMillis();
        for(int i = 0; i < TESTS; i++) {
            for(String name: conveyors.keySet()) {
                Conveyor<Integer, String, String> c = conveyors.get(name);
                assertTrue(c.isRunning());
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
                Conveyor<Integer, String, String> c = Conveyor.byName(name);
                assertTrue(c.isRunning());
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("jmxLookupPerfTest "+(end-start)/1000.0+"s");
    }

    private Map<String,Conveyor<Integer,String,String>> createConveyors(String namePrefix) {
        Map<String, Conveyor<Integer, String, String>> conveyors = new LinkedHashMap<>();

        for(int i = 0; i < CONVEYORS; i++) {
            AssemblingConveyor<Integer,String,String> c = new AssemblingConveyor<>();
            c.setName(namePrefix+(i+1));
            conveyors.put(c.getName(),c);
        }

        return conveyors;
    }

}
