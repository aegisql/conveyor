package com.aegisql.conveyor.service.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestParsingTest {

    @Test
    void parsesIsoOffsetWithHourOnly() {
        long expected = Instant.parse("2026-02-11T07:30:00Z").toEpochMilli();
        long parsed = RequestParsing.parseEpochMillis("2026-02-11T12:30:00+05").orElseThrow();
        assertEquals(expected, parsed);
    }

    @Test
    void parsesIsoOffsetWithCompactHourMinute() {
        long expected = Instant.parse("2026-02-11T07:00:00Z").toEpochMilli();
        long parsed = RequestParsing.parseEpochMillis("2026-02-11T12:30:00+0530").orElseThrow();
        assertEquals(expected, parsed);
    }

    @Test
    void parsesLocalDateAndDateTimeAsServerDefaultZone() {
        long dateTime = RequestParsing.parseEpochMillis("2026-02-11T12:30:00").orElseThrow();
        long dateOnly = RequestParsing.parseEpochMillis("2026-02-11").orElseThrow();
        ZoneId serverZone = ZoneId.systemDefault();
        long expectedDateTime = LocalDateTime.parse("2026-02-11T12:30:00")
                .atZone(serverZone)
                .toInstant()
                .toEpochMilli();
        long expectedDateOnly = LocalDate.parse("2026-02-11")
                .atStartOfDay(serverZone)
                .toInstant()
                .toEpochMilli();
        assertEquals(expectedDateTime, dateTime);
        assertEquals(expectedDateOnly, dateOnly);
    }

    @Test
    void parsesRfc1123DateTime() {
        long parsed = RequestParsing.parseEpochMillis("Wed, 11 Feb 2026 12:30:00 GMT").orElseThrow();
        assertEquals(Instant.parse("2026-02-11T12:30:00Z").toEpochMilli(), parsed);
    }

    @Test
    void rejectsInvalidDateTime() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RequestParsing.parseEpochMillis("not-a-time")
        );
        assertTrue(ex.getMessage().contains("Invalid datetime"));
    }
}
