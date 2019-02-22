package com.aegisql.conveyor.delay;

import org.junit.Test;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DelayBoxTest {
    @Test
    public void testEquality() {
        long now = System.currentTimeMillis();
        DelayBox<Integer> b1 = new DelayBox<>(now);
        DelayBox<Integer> b2 = new DelayBox<>(now);
        DelayBox<Integer> b3 = new DelayBox<>(now+10000);
        DelayBox<Integer> b4 = new DelayBox<>(now-10000);
        DelayBox<Integer> b5 = new DelayBox<>(0);
        assertEquals(b1,b1);
        assertEquals(b1,b2);
        assertNotEquals(b1,"string");
        assertNotEquals(b1,null);
        assertEquals(b1.hashCode(),b2.hashCode());
        assertNotEquals(b1,b3);
        assertNotEquals(b1.hashCode(),b3.hashCode());
        System.out.println(b1);

        Delayed d1 = b1.toDelayed();
        assertEquals(0,d1.compareTo(d1));
        assertEquals(-1,d1.compareTo(b3));
        assertEquals(1,d1.compareTo(b4));

        Delayed d2 = b5.toDelayed();
        assertEquals(Long.MAX_VALUE,d2.getDelay(TimeUnit.MILLISECONDS));
    }
}