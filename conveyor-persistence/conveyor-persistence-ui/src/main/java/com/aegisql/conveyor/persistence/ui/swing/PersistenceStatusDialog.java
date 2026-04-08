package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.SummaryEntry;
import com.aegisql.conveyor.persistence.ui.model.TableData;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

final class PersistenceStatusDialog extends JDialog {

    private PersistenceStatusDialog(
            Component owner,
            String title,
            ConnectionStatus currentStatus,
            PersistenceSnapshot snapshot,
            String fallbackText,
            String credentialStorageHint
    ) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel statusLabel = new JLabel("Current Connection Status: " + currentStatus);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        header.add(statusLabel, BorderLayout.NORTH);

        if (snapshot == null) {
            JTextArea fallbackArea = new JTextArea(fallbackText, 16, 72);
            fallbackArea.setEditable(false);
            fallbackArea.setLineWrap(true);
            fallbackArea.setWrapStyleWord(true);
            fallbackArea.setCaretPosition(0);
            add(header, BorderLayout.NORTH);
            add(new JScrollPane(fallbackArea), BorderLayout.CENTER);
        } else {
            if (!snapshot.headline().isBlank() || !snapshot.detailMessage().isBlank()) {
                JTextArea detailArea = new JTextArea(formatDetailText(snapshot, currentStatus), 4, 72);
                detailArea.setEditable(false);
                detailArea.setLineWrap(true);
                detailArea.setWrapStyleWord(true);
                detailArea.setCaretPosition(0);
                detailArea.setBorder(BorderFactory.createEmptyBorder());
                detailArea.setBackground(header.getBackground());
                header.add(detailArea, BorderLayout.CENTER);
            }
            add(header, BorderLayout.NORTH);
            add(tabbedContent(snapshot, credentialStorageHint), BorderLayout.CENTER);
        }

        setPreferredSize(new Dimension(860, 520));
        pack();
        setLocationRelativeTo(owner);
    }

    static void showDialog(
            Component owner,
            String title,
            ConnectionStatus currentStatus,
            PersistenceSnapshot snapshot,
            String fallbackText,
            String credentialStorageHint
    ) {
        new PersistenceStatusDialog(owner, title, currentStatus, snapshot, fallbackText, credentialStorageHint).setVisible(true);
    }

    private JTabbedPane tabbedContent(PersistenceSnapshot snapshot, String credentialStorageHint) {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Summary", tablePane(summaryTable(snapshot.summaryEntries(), credentialStorageHint)));
        for (TableData table : snapshot.infoTables()) {
            tabs.addTab(table.title(), tablePane(table));
        }
        return tabs;
    }

    private JScrollPane tablePane(TableData table) {
        JTable jTable = new JTable(table.toTableModel());
        WorkbenchTables.style(jTable);
        return new JScrollPane(jTable);
    }

    private TableData summaryTable(List<SummaryEntry> entries, String credentialStorageHint) {
        List<List<String>> rows = new ArrayList<>(entries.stream()
                .map(entry -> List.of(entry.label(), entry.value()))
                .toList());
        if (credentialStorageHint != null && !credentialStorageHint.isBlank()) {
            rows.add(List.of("Credential Storage", credentialStorageHint));
        }
        return new TableData("Summary", List.of("Field", "Value"), rows);
    }

    private String formatDetailText(PersistenceSnapshot snapshot, ConnectionStatus currentStatus) {
        StringBuilder builder = new StringBuilder();
        if (currentStatus != snapshot.status()) {
            builder.append("Last loaded snapshot status: ").append(snapshot.status()).append('\n');
        }
        if (!snapshot.headline().isBlank()) {
            builder.append(snapshot.headline()).append('\n');
        }
        if (!snapshot.detailMessage().isBlank()) {
            builder.append(snapshot.detailMessage());
        }
        return builder.toString().trim();
    }
}
