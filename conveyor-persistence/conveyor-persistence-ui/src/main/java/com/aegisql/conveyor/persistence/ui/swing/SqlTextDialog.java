package com.aegisql.conveyor.persistence.ui.swing;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlTextDialog extends JDialog {

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)\\b(create|table|index|schema|primary|key|foreign|references|constraint|unique|not|null|default|check|insert|into|values|update|set|delete|from|where|select|distinct|order|by|group|having|join|left|right|inner|outer|on|and|or|as|drop|alter|add|column|if|exists|view|grant|revoke|cascade|union|all|limit|offset)\\b"
    );
    private static final Pattern SQL_STRINGS = Pattern.compile("'([^']|'')*'");
    private static final Pattern SQL_LINE_COMMENTS = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern SQL_BLOCK_COMMENTS = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private SqlTextDialog(Component owner, String title, String text) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textPane.setBackground(Color.WHITE);
        applySqlHighlight(textPane, text);
        textPane.setCaretPosition(0);
        add(new JScrollPane(textPane), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(text), null));
        JButton saveButton = new JButton("Save to File...");
        saveButton.addActionListener(e -> saveToFile(text));
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

    private void applySqlHighlight(JTextPane textPane, String text) {
        StyledDocument document = textPane.getStyledDocument();
        try {
            document.remove(0, document.getLength());
            document.insertString(0, text, baseStyle());
            applyMatches(document, text, SQL_KEYWORDS, keywordStyle());
            applyMatches(document, text, SQL_STRINGS, stringStyle());
            applyMatches(document, text, SQL_LINE_COMMENTS, commentStyle());
            applyMatches(document, text, SQL_BLOCK_COMMENTS, commentStyle());
        } catch (BadLocationException e) {
            throw new IllegalStateException("Failed to render SQL text", e);
        }
    }

    private void applyMatches(StyledDocument document, String text, Pattern pattern, SimpleAttributeSet style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }

    private SimpleAttributeSet baseStyle() {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, new Color(17, 24, 39));
        StyleConstants.setFontFamily(style, Font.MONOSPACED);
        StyleConstants.setFontSize(style, 13);
        return style;
    }

    private SimpleAttributeSet keywordStyle() {
        SimpleAttributeSet style = baseStyle();
        StyleConstants.setForeground(style, new Color(124, 58, 237));
        StyleConstants.setBold(style, true);
        return style;
    }

    private SimpleAttributeSet stringStyle() {
        SimpleAttributeSet style = baseStyle();
        StyleConstants.setForeground(style, new Color(4, 120, 87));
        return style;
    }

    private SimpleAttributeSet commentStyle() {
        SimpleAttributeSet style = baseStyle();
        StyleConstants.setForeground(style, new Color(100, 116, 139));
        StyleConstants.setItalic(style, true);
        return style;
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
