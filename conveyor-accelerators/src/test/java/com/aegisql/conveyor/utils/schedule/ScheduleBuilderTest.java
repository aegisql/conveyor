package com.aegisql.conveyor.utils.schedule;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleBuilderTest {

    @Test
    void shouldConfigureRescheduleAndTimeoutBehavior() {
        AtomicInteger invocations = new AtomicInteger();
        SchedulableClosure closure = invocations::incrementAndGet;

        ScheduleBuilder<Integer> recurring = new ScheduleBuilder<>(50);
        long initialExpiration = recurring.getExpirationTime();
        ScheduleBuilder.setClosure(recurring, closure);
        assertTrue(recurring.isReschedule());
        assertSame(closure, recurring.get());

        recurring.onTimeout();
        assertEquals(1, invocations.get());
        assertFalse(recurring.test(null));
        assertTrue(recurring.getExpirationTime() >= initialExpiration);

        ScheduleBuilder<Integer> once = new ScheduleBuilder<>(50);
        ScheduleBuilder.setClosureOnce(once, closure);
        assertFalse(once.isReschedule());
        once.onTimeout();
        assertEquals(2, invocations.get());
        assertTrue(once.test(null));
    }

    @Test
    void setAndExecuteShouldRunImmediatelyAndStayRecurring() {
        AtomicInteger invocations = new AtomicInteger();
        SchedulableClosure closure = invocations::incrementAndGet;

        ScheduleBuilder<Integer> builder = new ScheduleBuilder<>(20);
        ScheduleBuilder.setAndExecuteClosure(builder, closure);

        assertNotNull(builder.get());
        assertEquals(1, invocations.get());
        assertTrue(builder.isReschedule());

        builder.onTimeout();
        assertEquals(2, invocations.get());
        assertFalse(builder.test(null));
    }
}
