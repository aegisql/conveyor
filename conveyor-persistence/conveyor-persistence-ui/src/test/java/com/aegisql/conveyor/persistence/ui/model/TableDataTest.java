package com.aegisql.conveyor.persistence.ui.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableDataTest {

    @Test
    void filtersHiddenColumnsWhilePreservingRowOrder() {
        TableData table = new TableData(
                "Parts",
                List.of("ID", "Label", "Payload", "Created"),
                List.of(
                        List.of("1", "TEXT1", "hello", "100"),
                        List.of("2", "TEXT2", "world", "200")
                )
        );

        TableData filtered = table.filteredColumns(Set.of("Payload", "Created"));

        assertEquals(List.of("ID", "Label"), filtered.columns());
        assertEquals(List.of("1", "TEXT1"), filtered.rows().getFirst());
        assertEquals(List.of("2", "TEXT2"), filtered.rows().get(1));
    }

    @Test
    void addsVirtualRowNumberColumn() {
        TableData table = new TableData(
                "Parts",
                List.of("ID", "Label"),
                List.of(
                        List.of("17", "TEXT1"),
                        List.of("18", "TEXT2")
                )
        );

        TableData numbered = table.withRowNumbers();

        assertEquals(List.of("#", "ID", "Label"), numbered.columns());
        assertEquals(List.of("1", "17", "TEXT1"), numbered.rows().getFirst());
        assertEquals(List.of("2", "18", "TEXT2"), numbered.rows().get(1));
    }

    @Test
    void addsVirtualRowNumberColumnWithPageOffset() {
        TableData table = new TableData(
                "Parts",
                List.of("ID", "Label"),
                List.of(
                        List.of("17", "TEXT1"),
                        List.of("18", "TEXT2")
                )
        );

        TableData numbered = table.withRowNumbers(201);

        assertEquals(List.of("#", "ID", "Label"), numbered.columns());
        assertEquals(List.of("201", "17", "TEXT1"), numbered.rows().getFirst());
        assertEquals(List.of("202", "18", "TEXT2"), numbered.rows().get(1));
    }
}
