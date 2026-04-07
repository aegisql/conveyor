package com.aegisql.conveyor.persistence.ui.swing;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class SqlTextDialog extends JDialog {

    private SqlTextDialog(Component owner, String title, String text) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        JTextArea textArea = new JTextArea(text, 32, 100);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(textArea.getText()), null));
        JButton saveButton = new JButton("Save to File...");
        saveButton.addActionListener(e -> saveToFile(textArea.getText()));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(copyButton);
        buttons.add(saveButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    static void showDialog(Component owner, String title, String text) {
        new SqlTextDialog(owner, title, text).setVisible(true);
    }

    private void saveToFile(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Initialization Script");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
