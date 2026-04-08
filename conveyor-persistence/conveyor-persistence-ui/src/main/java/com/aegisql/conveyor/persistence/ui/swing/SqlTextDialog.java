package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.backend.InitializationPreview;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SqlTextDialog extends JDialog {

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)\\b(create|table|index|schema|primary|key|foreign|references|constraint|unique|not|null|default|check|insert|into|values|update|set|delete|from|where|select|distinct|order|by|group|having|join|left|right|inner|outer|on|and|or|as|drop|alter|add|column|if|exists|view|grant|revoke|cascade|union|all|limit|offset)\\b"
    );
    private static final Pattern SQL_STRINGS = Pattern.compile("'([^']|'')*'");
    private static final Pattern SQL_LINE_COMMENTS = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern SQL_BLOCK_COMMENTS = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern JAVA_KEYWORDS = Pattern.compile(
            "\\b(import|package|public|private|protected|final|class|static|void|new|try|catch|throws|throw|if|else|return)\\b"
    );
    private static final Pattern JAVA_STRINGS = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern JAVA_LINE_COMMENTS = Pattern.compile("//.*$", Pattern.MULTILINE);
    private static final Pattern JAVA_BLOCK_COMMENTS = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private final JTabbedPane tabs = new JTabbedPane();
    private final List<TabContent> contents;

    private SqlTextDialog(Component owner, String title, InitializationPreview preview) {
        super(SwingUtilities.getWindowAncestor(owner), title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));
        this.contents = buildContents(preview);

        for (TabContent content : contents) {
            JTextPane textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            textPane.setBackground(Color.WHITE);
            applyHighlight(textPane, content);
            textPane.setCaretPosition(0);
            tabs.addTab(content.title(), new JScrollPane(textPane));
        }
        add(tabs, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(currentContent().text()), null));
        JButton saveButton = new JButton("Save to File...");
        saveButton.addActionListener(e -> saveToFile(currentContent()));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(copyButton);
        buttons.add(saveButton);
        buttons.add(closeButton);
        add(buttons, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(980, 680));
        pack();
        setLocationRelativeTo(owner);
    }

    static void showDialog(Component owner, String title, InitializationPreview preview) {
        new SqlTextDialog(owner, title, preview).setVisible(true);
    }

    private void applyHighlight(JTextPane textPane, TabContent content) {
        String text = content.text();
        StyledDocument document = textPane.getStyledDocument();
        try {
            document.remove(0, document.getLength());
            document.insertString(0, text, baseStyle());
            if (content.syntax() == Syntax.SQL) {
                applyMatches(document, text, SQL_KEYWORDS, keywordStyle());
                applyMatches(document, text, SQL_STRINGS, stringStyle());
                applyMatches(document, text, SQL_LINE_COMMENTS, commentStyle());
                applyMatches(document, text, SQL_BLOCK_COMMENTS, commentStyle());
            } else {
                applyMatches(document, text, JAVA_KEYWORDS, keywordStyle());
                applyMatches(document, text, JAVA_STRINGS, stringStyle());
                applyMatches(document, text, JAVA_LINE_COMMENTS, commentStyle());
                applyMatches(document, text, JAVA_BLOCK_COMMENTS, commentStyle());
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("Failed to render preview text", e);
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

    private void saveToFile(TabContent content) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Initialization Preview");
        chooser.setSelectedFile(new File(defaultFileName(content)));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), content.text(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                javax.swing.JOptionPane.showMessageDialog(this, e.getMessage(), "Save Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private TabContent currentContent() {
        int index = Math.max(0, tabs.getSelectedIndex());
        return contents.get(index);
    }

    private String defaultFileName(TabContent content) {
        return content.syntax() == Syntax.SQL ? "init-persistence.sql" : "InitPersistenceExample.java";
    }

    private List<TabContent> buildContents(InitializationPreview preview) {
        List<TabContent> tabContents = new ArrayList<>();
        if (preview.sql() != null && !preview.sql().isBlank()) {
            tabContents.add(new TabContent("SQL", preview.sql(), Syntax.SQL));
        }
        if (preview.java() != null && !preview.java().isBlank()) {
            tabContents.add(new TabContent("Java", preview.java(), Syntax.JAVA));
        }
        if (tabContents.isEmpty()) {
            tabContents.add(new TabContent("Preview", "No initialization preview available.", Syntax.JAVA));
        }
        return List.copyOf(tabContents);
    }

    private record TabContent(String title, String text, Syntax syntax) {
    }

    private enum Syntax {
        SQL,
        JAVA
    }
}
