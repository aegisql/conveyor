package com.aegisql.conveyor.utils.counter;

import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CounterTest {

    @Test
    public void testCounter() {
        Counter counter = new Counter("testCounter");

        assertFalse(counter.test());

        counter.add(5);
        assertEquals(5, counter.getCount());
        assertFalse(counter.test());

        counter.setExpected(5);
        assertTrue(counter.test());

        counter.add(3);
        assertEquals(8, counter.getCount());
        assertThrows(IllegalStateException.class,counter::test);

        assertThrows(KeepRunningConveyorException.class,()->counter.setExpected(8));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            counter.add(-1);
        });
        assertEquals("Counter testCounter cannot be decremented, value: -1", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            counter.setExpected(-1);
        });
        assertEquals("Counter testCounter cannot have negative expected value: -1", exception.getMessage());
    }

    @Test
    public void testCounterAccessorsAndToString() {
        Counter counter = new Counter("counterAccessors");

        assertEquals("counterAccessors", counter.getName());
        assertEquals(0, counter.getCount());
        assertEquals(-1, counter.getExpected());

        counter.setExpected(0);
        assertEquals(0, counter.getExpected());
        assertTrue(counter.test());

        // Setting the same expected value again is allowed.
        counter.setExpected(0);
        assertEquals(0, counter.getExpected());

        String text = counter.toString();
        assertTrue(text.contains("name='counterAccessors'"));
        assertTrue(text.contains("count=0"));
        assertTrue(text.contains("expected=0"));
    }

}
