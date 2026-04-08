package com.aegisql.conveyor.persistence.ui.model;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

public record TableData(String title, List<String> columns, List<List<String>> rows) {

    public static final String ROW_NUMBER_COLUMN = "#";

    public TableData {
        Objects.requireNonNull(title, "title must not be null");
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public static TableData empty(String title, List<String> columns) {
        return new TableData(title, columns, List.of());
    }

    public TableData filteredColumns(Set<String> hiddenColumns) {
        if (hiddenColumns == null || hiddenColumns.isEmpty()) {
            return this;
        }
        List<Integer> visibleIndexes = new ArrayList<>();
        List<String> visibleColumns = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            if (!hiddenColumns.contains(column)) {
                visibleIndexes.add(i);
                visibleColumns.add(column);
            }
        }
        if (visibleIndexes.size() == columns.size()) {
            return this;
        }
        List<List<String>> visibleRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> visibleRow = new ArrayList<>(visibleIndexes.size());
            for (Integer index : visibleIndexes) {
                visibleRow.add(row.get(index));
            }
            visibleRows.add(visibleRow);
        }
        return new TableData(title, visibleColumns, visibleRows);
    }

    public TableData withRowNumbers() {
        return withRowNumbers(1);
    }

    public TableData withRowNumbers(int firstRowNumber) {
        if (!columns.isEmpty() && ROW_NUMBER_COLUMN.equals(columns.getFirst())) {
            return this;
        }
        int normalizedFirstRowNumber = Math.max(1, firstRowNumber);
        List<String> numberedColumns = new ArrayList<>(columns.size() + 1);
        numberedColumns.add(ROW_NUMBER_COLUMN);
        numberedColumns.addAll(columns);

        List<List<String>> numberedRows = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            List<String> numberedRow = new ArrayList<>(rows.get(i).size() + 1);
            numberedRow.add(Integer.toString(normalizedFirstRowNumber + i));
            numberedRow.addAll(rows.get(i));
            numberedRows.add(numberedRow);
        }
        return new TableData(title, numberedColumns, numberedRows);
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
