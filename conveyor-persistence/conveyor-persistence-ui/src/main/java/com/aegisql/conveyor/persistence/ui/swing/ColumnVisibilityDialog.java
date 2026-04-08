package com.aegisql.conveyor.persistence.ui.swing;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ColumnVisibilityDialog {

    private ColumnVisibilityDialog() {
    }

    static Optional<Set<String>> chooseVisibleColumns(Component owner, String tableTitle, List<String> columns, Set<String> visibleColumns) {
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        Set<JCheckBox> checkBoxes = new LinkedHashSet<>();
        for (String column : columns) {
            JCheckBox checkBox = new JCheckBox(column, visibleColumns.contains(column));
            checkBoxes.add(checkBox);
            checkboxPanel.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(tableTitle));
        scrollPane.setPreferredSize(new Dimension(320, 220));

        int answer = JOptionPane.showConfirmDialog(
                owner,
                scrollPane,
                "Visible Columns",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (answer != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        Set<String> selectedColumns = new LinkedHashSet<>();
        for (JCheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                selectedColumns.add(checkBox.getText());
            }
        }
        return Optional.of(selectedColumns);
    }
}
