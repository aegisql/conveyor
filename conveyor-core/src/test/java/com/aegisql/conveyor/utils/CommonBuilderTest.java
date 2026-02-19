package com.aegisql.conveyor.utils;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonBuilderTest {

    static class TestCommonBuilder extends CommonBuilder<String> {
        TestCommonBuilder(long ttl, TimeUnit unit) {
            super(ttl, unit);
        }

        TestCommonBuilder(long expirationTime) {
            super(expirationTime);
        }

        TestCommonBuilder(Duration duration) {
            super(duration);
        }

        TestCommonBuilder(Instant instant) {
            super(instant);
        }

        TestCommonBuilder() {
            super();
        }

        @Override
        public String get() {
            return "value";
        }
    }

    @Test
    void shouldSupportAllConstructorsAndReadyState() {
        long before = System.currentTimeMillis();
        TestCommonBuilder ttlBuilder = new TestCommonBuilder(2, TimeUnit.SECONDS);
        assertTrue(ttlBuilder.getExpirationTime() >= before + 1900);

        long explicitExpiration = System.currentTimeMillis() + 1234;
        TestCommonBuilder expirationBuilder = new TestCommonBuilder(explicitExpiration);
        assertEquals(explicitExpiration, expirationBuilder.getExpirationTime());

        TestCommonBuilder durationBuilder = new TestCommonBuilder(Duration.ofMillis(500));
        assertTrue(durationBuilder.getExpirationTime() >= System.currentTimeMillis() + 400);

        Instant instant = Instant.now().plusSeconds(3);
        TestCommonBuilder instantBuilder = new TestCommonBuilder(instant);
        assertEquals(instant.toEpochMilli(), instantBuilder.getExpirationTime());

        TestCommonBuilder defaultBuilder = new TestCommonBuilder();
        assertEquals(0L, defaultBuilder.getExpirationTime());

        assertFalse(defaultBuilder.test());
        defaultBuilder.setReady(true);
        assertTrue(defaultBuilder.test());
        assertEquals("value", defaultBuilder.get());
    }
}
