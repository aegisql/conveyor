package com.aegisql.conveyor.persistence.ui.model;

import java.util.Objects;

public record SummaryEntry(String label, String value) {
    public SummaryEntry {
        Objects.requireNonNull(label, "label must not be null");
        value = value == null ? "" : value;
    }
}
