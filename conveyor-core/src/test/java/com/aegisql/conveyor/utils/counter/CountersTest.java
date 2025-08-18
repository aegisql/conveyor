package com.aegisql.conveyor.utils.counter;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CountersTest {

    @Test
    public void testCounters() {
        Counters counters = new Counters();
        counters.add("test1", 5);
        counters.add("test2", 10);
        counters.add("test1", 3);

        assertEquals(8, counters.getCounters().get("test1"));
        assertEquals(10, counters.getCounters().get("test2"));

        assertTrue(counters.containsAllNames(Set.of("test1", "test2")));
        assertFalse(counters.containsAllNames(Set.of("test1", "test3")));

        Counters anotherCounters = new Counters();
        anotherCounters.add("test1", 8);
        anotherCounters.add("test2", 10);

        assertEquals(counters, anotherCounters);

        anotherCounters.add("test1", 8);
        assertNotEquals(counters, anotherCounters);

        assertNotEquals(counters, null);
        assertNotEquals(counters, new Object());

        System.out.println(counters);
    }

}