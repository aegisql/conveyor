package com.aegisql.conveyor.persistence.ui.model;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public record TableData(String title, List<String> columns, List<List<String>> rows) {

    public TableData {
        Objects.requireNonNull(title, "title must not be null");
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public static TableData empty(String title, List<String> columns) {
        return new TableData(title, columns, List.of());
    }

    public DefaultTableModel toTableModel() {
        Vector<String> columnVector = new Vector<>(columns);
        Vector<Vector<String>> dataVector = new Vector<>();
        for (List<String> row : rows) {
            dataVector.add(new Vector<>(row));
        }
        return new DefaultTableModel(dataVector, columnVector) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
