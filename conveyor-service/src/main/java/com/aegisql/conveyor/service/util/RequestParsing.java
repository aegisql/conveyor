package com.aegisql.conveyor.service.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

public final class RequestParsing {
    private RequestParsing() {}
    private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.RFC_1123_DATE_TIME,
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .appendOffset("+HH", "Z")
                    .toFormatter(Locale.ROOT),
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .appendOffset("+HHMM", "Z")
                    .toFormatter(Locale.ROOT),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    public static OptionalLong parseDurationMillis(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        var trimmed = raw.trim();
        if (trimmed.matches("^\\d+$")) {
            return OptionalLong.of(Long.parseLong(trimmed));
        }
        var parts = trimmed.split("\\s+");
        if (parts.length == 2 && parts[0].matches("^\\d+$")) {
            var value = Long.parseLong(parts[0]);
            try {
                var unit = TimeUnit.valueOf(parts[1].toUpperCase(Locale.ROOT));
                return OptionalLong.of(TimeUnit.MILLISECONDS.convert(value, unit));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid duration unit: " + parts[1], ex);
            }
        }
        throw new IllegalArgumentException("Invalid duration format: " + raw);
    }

    public static OptionalLong parseEpochMillis(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        var trimmed = raw.trim();
        if (trimmed.matches("^\\d+$")) {
            return OptionalLong.of(Long.parseLong(trimmed));
        }
        for (String candidate : dateTimeCandidates(trimmed)) {
            for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
                try {
                    TemporalAccessor parsed = formatter.parseBest(
                            candidate,
                            Instant::from,
                            OffsetDateTime::from,
                            ZonedDateTime::from,
                            LocalDateTime::from,
                            LocalDate::from
                    );
                    return OptionalLong.of(toEpochMillis(parsed));
                } catch (DateTimeParseException ignored) {
                    // Try the next known formatter.
                }
            }
        }
        throw new IllegalArgumentException("Invalid datetime: " + raw);
    }

    private static long toEpochMillis(TemporalAccessor parsed) {
        if (parsed instanceof Instant instant) {
            return instant.toEpochMilli();
        }
        if (parsed instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant().toEpochMilli();
        }
        if (parsed instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toInstant().toEpochMilli();
        }
        if (parsed instanceof LocalDateTime localDateTime) {
            // No timezone provided: use server default timezone.
            return localDateTime.atZone(SERVER_ZONE).toInstant().toEpochMilli();
        }
        if (parsed instanceof LocalDate localDate) {
            // Date-only values are interpreted as midnight in server default timezone.
            return localDate.atStartOfDay(SERVER_ZONE).toInstant().toEpochMilli();
        }
        throw new IllegalArgumentException("Unsupported datetime value");
    }

    private static List<String> dateTimeCandidates(String value) {
        List<String> candidates = new ArrayList<>(2);
        candidates.add(value);
        if (value.length() > 10 && value.charAt(10) == ' ') {
            candidates.add(value.substring(0, 10) + "T" + value.substring(11));
        }
        return candidates;
    }
}
