package com.aegisql.conveyor.persistence.ui.swing;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

final class LookupSelectionDialog extends JDialog {

    private String selectedValue;

    private LookupSelectionDialog(Component owner, String title, List<String> values) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        DefaultListModel<String> model = new DefaultListModel<>();
        values.forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (!values.isEmpty()) {
            list.setSelectedIndex(0);
        }
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !list.isSelectionEmpty()) {
                    selectedValue = list.getSelectedValue();
                    dispose();
                }
            }
        });

        add(new JScrollPane(list), BorderLayout.CENTER);
        add(buttonPanel(list), BorderLayout.SOUTH);

        setSize(420, 320);
        setLocationRelativeTo(owner);
    }

    static Optional<String> choose(Component owner, String title, List<String> values) {
        LookupSelectionDialog dialog = new LookupSelectionDialog(owner, title, values);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.selectedValue);
    }

    private JPanel buttonPanel(JList<String> list) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        selectButton.addActionListener(e -> {
            selectedValue = list.getSelectedValue();
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedValue = null;
            dispose();
        });
        panel.add(selectButton);
        panel.add(cancelButton);
        getRootPane().setDefaultButton(selectButton);
        return panel;
    }
}
