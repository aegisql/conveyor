package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.model.TableData;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

final class WorkbenchTables {

    private static final Color HEADER_BACKGROUND = new Color(226, 232, 240);
    private static final Color HEADER_FOREGROUND = new Color(30, 41, 59);
    private static final Color HEADER_BORDER = new Color(203, 213, 225);
    private static final Color EVEN_ROW_BACKGROUND = new Color(245, 250, 255);

    private WorkbenchTables() {
    }

    static void style(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setRowHeight(Math.max(table.getRowHeight(), 24));
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(226, 232, 240));
        styleHeader(table.getTableHeader());
        styleCells(table);
    }

    private static void styleHeader(JTableHeader header) {
        Font font = header.getFont();
        header.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 1f));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(HEADER_BACKGROUND);
                setForeground(HEADER_FOREGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, HEADER_BORDER),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
                setHorizontalAlignment(TableData.ROW_NUMBER_COLUMN.equals(value) ? CENTER : LEFT);
                return this;
            }
        });
    }

    private static void styleCells(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable currentTable,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                super.getTableCellRendererComponent(currentTable, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : EVEN_ROW_BACKGROUND);
                    setForeground(currentTable.getForeground());
                }
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                setHorizontalAlignment(
                        TableData.ROW_NUMBER_COLUMN.equals(currentTable.getColumnName(column)) ? CENTER : LEFT
                );
                return this;
            }
        });
    }
}
