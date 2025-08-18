package com.aegisql.conveyor.utils.counter;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountersAggregatorTest {

    @Test
    public void testCountersAggregator() {
        CountersAggregator aggregator = new CountersAggregator();
        assertFalse(aggregator.test());

        aggregator.addNames(List.of("task1","task2"));

        aggregator.addExpected("task1", 5);
        aggregator.addExpected("task2", 10);

        aggregator.addCounter("task1", 3);
        aggregator.addCounter("task2", 10);

        assertFalse(aggregator.test());
        System.out.println(aggregator.get());

        aggregator.addCounter("task1", 2);

        assertTrue(aggregator.test());

        var result = aggregator.get();
        assertEquals(2, result.size());
        assertEquals(5, result.get("task1").get("expected"));
        assertEquals(5, result.get("task1").get("actual"));
        assertEquals(10, result.get("task2").get("expected"));
        assertEquals(10, result.get("task2").get("actual"));

        aggregator.addCounter("task1", 1);
        assertThrows(IllegalStateException.class,aggregator::test);
        System.out.println(aggregator);
        System.out.println(result);
    }

}