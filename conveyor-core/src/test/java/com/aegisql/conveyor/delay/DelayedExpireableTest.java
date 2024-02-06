package com.aegisql.conveyor.delay;

import com.aegisql.conveyor.Expireable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DelayedExpireableTest {

    @Test
    public void delayedExpireableTest() {

        final long expTime = System.currentTimeMillis()+1000;

        DelayedExpireable de1 = new DelayedExpireable(expTime);
        DelayedExpireable de2 = new DelayedExpireable(Instant.ofEpochMilli(expTime));
        DelayedExpireable de3 = new DelayedExpireable(new Expireable() {
            @Override
            public long getExpirationTime() {
                return expTime;
            }
        });
        assertEquals(de1,de2);
        assertEquals(de1.hashCode(),de2.hashCode());
        assertEquals(de1.getExpirationTime(),de2.getExpirationTime());
        assertEquals(de1,de3);
        DelayedExpireable de4 = new DelayedExpireable(expTime+100000);
        DelayedExpireable de5 = new DelayedExpireable(expTime-100000);

        int cmp1 = de1.compareTo(de2);
        int cmp2 = de1.compareTo(de4);
        int cmp3 = de1.compareTo(de5);

        assertTrue(cmp2 < 0 );
        assertTrue(cmp3 > 0 );

        long delay1 = de1.getDelay(TimeUnit.MILLISECONDS);
        long delay2 = de2.getDelay(TimeUnit.MILLISECONDS);
        assertTrue(delay1 > 0);
        assertTrue(delay2 > 0);
        assertTrue(delay1 <= 1000);
        assertTrue(delay2 <= 1000);
        System.out.println(de1);
    }

}