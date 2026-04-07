package com.aegisql.conveyor.persistence.ui.model;

import java.util.List;
import java.util.Objects;

public record PersistenceSnapshot(
        ConnectionStatus status,
        String headline,
        String detailMessage,
        List<SummaryEntry> summaryEntries,
        List<TableData> tables,
        List<TableData> infoTables,
        int pageIndex,
        boolean hasPreviousPage,
        boolean hasNextPage,
        boolean canInitialize,
        boolean canGenerateInitializationScript,
        boolean canArchiveExpired,
        boolean canArchiveAll
) {
    public PersistenceSnapshot {
        Objects.requireNonNull(status, "status must not be null");
        headline = headline == null ? "" : headline;
        detailMessage = detailMessage == null ? "" : detailMessage;
        summaryEntries = List.copyOf(summaryEntries);
        tables = List.copyOf(tables);
        infoTables = List.copyOf(infoTables);
        pageIndex = Math.max(0, pageIndex);
    }
}
