package com.aegisql.conveyor.persistence.ui.swing;

import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackend;
import com.aegisql.conveyor.persistence.ui.backend.PersistenceBackendFactory;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;
import java.util.Optional;

final class ProfileEditorDialog extends JDialog {

    private final JTextField displayNameField = new JTextField(30);
    private final JComboBox<PersistenceKind> kindCombo = new JComboBox<>(PersistenceKind.values());
    private final JTextField keyClassField = new JTextField(30);
    private final JTextField persistenceNameField = new JTextField(30);
    private final JTextField hostField = new JTextField(30);
    private final JTextField portField = new JTextField(10);
    private final JTextField databaseField = new JTextField(30);
    private final JTextField schemaField = new JTextField(30);
    private final JTextField partTableField = new JTextField(30);
    private final JTextField completedLogTableField = new JTextField(30);
    private final JTextField userField = new JTextField(30);
    private final JPasswordField passwordField = new JPasswordField(30);
    private final JTextField redisUriField = new JTextField(30);
    private final JComboBox<PayloadEncryptionMode> payloadEncryptionModeCombo = new JComboBox<>(PayloadEncryptionMode.values());
    private final JPasswordField encryptionSecretField = new JPasswordField(30);
    private final JButton persistenceLookupButton = new JButton("Lookup");
    private final JButton databaseLookupButton = new JButton("Lookup");
    private final JButton schemaLookupButton = new JButton("Lookup");
    private final JLabel databaseLabel = new JLabel("Database");
    private final JLabel schemaLabel = new JLabel("Schema");
    private final JLabel persistenceNameLabel = new JLabel("Persistence Name");
    private final FormRow keyClassRow;
    private final FormRow persistenceNameRow;
    private final FormRow hostRow;
    private final FormRow portRow;
    private final FormRow databaseRow;
    private final FormRow schemaRow;
    private final FormRow partTableRow;
    private final FormRow completedLogTableRow;
    private final FormRow userRow;
    private final FormRow passwordRow;
    private final FormRow redisUriRow;
    private final FormRow payloadEncryptionModeRow;
    private final FormRow encryptionSecretRow;

    private PersistenceProfile value;
    private PersistenceKind currentKind;
    private String lastSuggestedDisplayName;
    private boolean displayNameCustomized;
    private boolean updatingDisplayName;

    private ProfileEditorDialog(Component owner, PersistenceProfile profile) {
        super(SwingUtilities.getWindowAncestor(owner), "Persistence Profile", ModalityType.APPLICATION_MODAL);
        this.value = profile.normalized();
        this.keyClassRow = new FormRow(new JLabel("Key Class"), keyClassField);
        this.persistenceNameRow = new FormRow(persistenceNameLabel, fieldPanel(persistenceNameField, persistenceLookupButton));
        this.hostRow = new FormRow(new JLabel("Host"), hostField);
        this.portRow = new FormRow(new JLabel("Port"), portField);
        this.databaseRow = new FormRow(databaseLabel, fieldPanel(databaseField, databaseLookupButton));
        this.schemaRow = new FormRow(schemaLabel, fieldPanel(schemaField, schemaLookupButton));
        this.partTableRow = new FormRow(new JLabel("Part Table"), partTableField);
        this.completedLogTableRow = new FormRow(new JLabel("Completed Log Table"), completedLogTableField);
        this.userRow = new FormRow(new JLabel("User"), userField);
        this.passwordRow = new FormRow(new JLabel("Password"), passwordField);
        this.redisUriRow = new FormRow(new JLabel("Redis URI"), redisUriField);
        this.payloadEncryptionModeRow = new FormRow(new JLabel("Value Encryption"), payloadEncryptionModeCombo);
        this.encryptionSecretRow = new FormRow(new JLabel("Encryption Secret"), encryptionSecretField);
        setLayout(new BorderLayout(8, 8));
        add(formPanel(), BorderLayout.CENTER);
        add(buttonPanel(), BorderLayout.SOUTH);
        populate(profile.normalized());
        currentKind = profile.normalized().kind();
        initDisplayNameTracking();
        wireLookupActions();
        kindCombo.addActionListener(e -> {
            PersistenceKind selectedKind = (PersistenceKind) kindCombo.getSelectedItem();
            if (selectedKind == null) {
                return;
            }
            applyDefaultsForKindChange(currentKind, selectedKind);
            applyKind(selectedKind);
            currentKind = selectedKind;
            updateDisplayNameSuggestion();
        });
        applyKind(currentKind);
        pack();
        setLocationRelativeTo(owner);
    }

    static Optional<PersistenceProfile> edit(Component owner, PersistenceProfile profile) {
        ProfileEditorDialog dialog = new ProfileEditorDialog(owner, profile);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.value);
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        row = addRow(panel, gbc, row, "Display Name", displayNameField);
        row = addRow(panel, gbc, row, "Type", kindCombo);
        row = addRow(panel, gbc, row, keyClassRow);
        row = addRow(panel, gbc, row, persistenceNameRow);
        row = addRow(panel, gbc, row, hostRow);
        row = addRow(panel, gbc, row, portRow);
        row = addRow(panel, gbc, row, databaseRow);
        row = addRow(panel, gbc, row, schemaRow);
        row = addRow(panel, gbc, row, partTableRow);
        row = addRow(panel, gbc, row, completedLogTableRow);
        row = addRow(panel, gbc, row, userRow);
        row = addRow(panel, gbc, row, passwordRow);
        row = addRow(panel, gbc, row, redisUriRow);
        row = addRow(panel, gbc, row, payloadEncryptionModeRow);
        addRow(panel, gbc, row, encryptionSecretRow);
        return panel;
    }

    private JPanel fieldPanel(JTextField field, JButton actionButton) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(field, BorderLayout.CENTER);
        panel.add(actionButton, BorderLayout.EAST);
        return panel;
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        return addRow(panel, gbc, row, new JLabel(label), field);
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int row, JLabel label, java.awt.Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        return row + 1;
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int row, FormRow formRow) {
        return addRow(panel, gbc, row, formRow.label(), formRow.field());
    }

    private JPanel buttonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveAndClose());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            value = null;
            dispose();
        });
        panel.add(saveButton);
        panel.add(cancelButton);
        getRootPane().setDefaultButton(saveButton);
        return panel;
    }

    private void populate(PersistenceProfile profile) {
        displayNameField.setText(profile.displayName());
        kindCombo.setSelectedItem(profile.kind());
        keyClassField.setText(nullToEmpty(profile.keyClassName()));
        persistenceNameField.setText(nullToEmpty(profile.persistenceName()));
        hostField.setText(nullToEmpty(profile.host()));
        portField.setText(profile.port() == null ? "" : Integer.toString(profile.port()));
        databaseField.setText(nullToEmpty(profile.database()));
        schemaField.setText(nullToEmpty(profile.schema()));
        partTableField.setText(nullToEmpty(profile.partTable()));
        completedLogTableField.setText(nullToEmpty(profile.completedLogTable()));
        userField.setText(nullToEmpty(profile.user()));
        passwordField.setText(nullToEmpty(profile.password()));
        redisUriField.setText(nullToEmpty(profile.redisUri()));
        payloadEncryptionModeCombo.setSelectedItem(profile.payloadEncryptionMode());
        encryptionSecretField.setText(nullToEmpty(profile.encryptionSecret()));
    }

    private void applyKind(PersistenceKind kind) {
        keyClassField.setEnabled(kind.showsKeyClassField());
        persistenceNameField.setEnabled(kind.showsPersistenceNameField());
        hostField.setEnabled(kind.showsHostField());
        portField.setEnabled(kind.showsPortField());
        databaseField.setEnabled(kind.showsDatabaseField());
        schemaField.setEnabled(kind.showsSchemaField());
        partTableField.setEnabled(kind.showsPartTableField());
        completedLogTableField.setEnabled(kind.showsCompletedLogTableField());
        userField.setEnabled(kind.showsUserField());
        passwordField.setEnabled(kind.showsPasswordField());
        redisUriField.setEnabled(kind.showsRedisUriField());
        databaseLookupButton.setEnabled(kind.showsDatabaseField());
        schemaLookupButton.setEnabled(kind.usesSchemaLookup());
        persistenceLookupButton.setEnabled(kind.showsPersistenceNameField());
        schemaLookupButton.setVisible(kind.usesSchemaLookup());

        keyClassRow.setVisible(kind.showsKeyClassField());
        persistenceNameRow.setVisible(kind.showsPersistenceNameField());
        hostRow.setVisible(kind.showsHostField());
        portRow.setVisible(kind.showsPortField());
        databaseRow.setVisible(kind.showsDatabaseField());
        schemaRow.setVisible(kind.showsSchemaField());
        partTableRow.setVisible(kind.showsPartTableField());
        completedLogTableRow.setVisible(kind.showsCompletedLogTableField());
        userRow.setVisible(kind.showsUserField());
        passwordRow.setVisible(kind.showsPasswordField());
        redisUriRow.setVisible(kind.showsRedisUriField());
        payloadEncryptionModeRow.setVisible(true);
        encryptionSecretRow.setVisible(true);

        databaseLabel.setText(kind.databaseFieldLabel());
        schemaLabel.setText(kind.schemaFieldLabel());
        databaseLookupButton.setText(kind.databaseActionLabel());
        databaseLookupButton.setToolTipText(kind.databaseActionToolTip());
        persistenceNameLabel.setText(kind.isRedis() ? "Persistence Name / Namespace" : "Persistence Name (Redis)");
        persistenceNameField.setToolTipText(kind.isRedis()
                ? "Use the Redis persistence name that created keys like conv:{name}:meta. This is not the runtime conveyor name."
                : null);
        schemaField.setToolTipText(kind == PersistenceKind.DERBY
                ? "Derby uses this as the database name created inside the selected home directory."
                : null);
        payloadEncryptionModeCombo.setToolTipText("Choose the stored-value encryption mode used by this persistence so the viewer can try to decrypt cart values.");
        encryptionSecretField.setToolTipText("Optional secret used to decrypt encrypted cart values during preview.");
        pack();
    }

    private void wireLookupActions() {
        persistenceLookupButton.setToolTipText("Lookup Redis persistence namespaces");
        databaseLookupButton.setToolTipText("Lookup available databases");
        schemaLookupButton.setToolTipText("Lookup available schemas");

        persistenceLookupButton.addActionListener(e -> lookupValues(
                "Redis Persistence Names",
                draftProfile -> PersistenceBackendFactory.forProfile(draftProfile).lookupPersistenceNames(draftProfile),
                persistenceNameField
        ));
        databaseLookupButton.addActionListener(e -> handleDatabaseAction());
        schemaLookupButton.addActionListener(e -> lookupValues(
                "Schemas",
                draftProfile -> PersistenceBackendFactory.forProfile(draftProfile).lookupSchemas(draftProfile),
                schemaField
        ));
    }

    private void handleDatabaseAction() {
        PersistenceKind kind = (PersistenceKind) kindCombo.getSelectedItem();
        if (kind == null || !kind.showsDatabaseField()) {
            return;
        }
        if (kind.usesLocalDatabasePath()) {
            browseLocalDatabase(kind);
            return;
        }
        lookupValues(
                "Databases",
                draftProfile -> PersistenceBackendFactory.forProfile(draftProfile).lookupDatabases(draftProfile),
                databaseField
        );
    }

    private void browseLocalDatabase(PersistenceKind kind) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(kind.databaseChooserTitle());
        chooser.setFileSelectionMode(kind.usesDirectoryDatabasePath() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        chooser.setApproveButtonText("Select");
        chooser.setSelectedFile(initialDatabaseSelection(kind));
        int result = chooser.showDialog(this, "Select");
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            databaseField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private File initialDatabaseSelection(PersistenceKind kind) {
        String currentValue = trimToNull(databaseField.getText());
        if (currentValue != null) {
            return new File(currentValue);
        }
        String defaultValue = kind.defaultDatabase();
        return defaultValue == null ? new File(System.getProperty("user.home")) : new File(defaultValue);
    }

    private void lookupValues(String title, java.util.function.Function<PersistenceProfile, List<String>> lookup, JTextField targetField) {
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            PersistenceProfile draft = draftProfile();
            List<String> values = lookup.apply(draft);
            if (values.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No values were found for the current connection settings.",
                        title,
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            LookupSelectionDialog.choose(this, title, values).ifPresent(targetField::setText);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    rootMessage(e),
                    title + " Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void applyDefaultsForKindChange(PersistenceKind previousKind, PersistenceKind nextKind) {
        PersistenceProfile previousDefaults = previousKind == null ? null : PersistenceProfile.defaults(previousKind).normalized();
        PersistenceProfile nextDefaults = PersistenceProfile.defaults(nextKind).normalized();

        applyDefault(keyClassField, previousDefaults == null ? null : previousDefaults.keyClassName(), nextDefaults.keyClassName());
        applyDefault(persistenceNameField, previousDefaults == null ? null : previousDefaults.persistenceName(), nextDefaults.persistenceName());
        applyDefault(hostField, previousDefaults == null ? null : previousDefaults.host(), nextDefaults.host());
        applyDefault(portField, previousDefaults == null || previousDefaults.port() == null ? null : Integer.toString(previousDefaults.port()),
                nextDefaults.port() == null ? null : Integer.toString(nextDefaults.port()));
        applyDefault(databaseField, previousDefaults == null ? null : previousDefaults.database(), nextDefaults.database());
        applyDefault(schemaField, previousDefaults == null ? null : previousDefaults.schema(), nextDefaults.schema());
        applyDefault(partTableField, previousDefaults == null ? null : previousDefaults.partTable(), nextDefaults.partTable());
        applyDefault(completedLogTableField, previousDefaults == null ? null : previousDefaults.completedLogTable(), nextDefaults.completedLogTable());
        applyDefault(redisUriField, previousDefaults == null ? null : previousDefaults.redisUri(), nextDefaults.redisUri());
    }

    private void applyDefault(JTextField field, String previousDefault, String nextDefault) {
        String current = trimToNull(field.getText());
        String oldDefault = trimToNull(previousDefault);
        if (current == null || (oldDefault != null && oldDefault.equals(current))) {
            field.setText(nullToEmpty(nextDefault));
        }
    }

    private void initDisplayNameTracking() {
        lastSuggestedDisplayName = suggestedDisplayName();
        displayNameCustomized = isCustomDisplayName();
        displayNameField.getDocument().addDocumentListener(new SimpleDocumentListener(this::handleDisplayNameEdited));
        databaseField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateDisplayNameSuggestion));
        partTableField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateDisplayNameSuggestion));
        persistenceNameField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateDisplayNameSuggestion));
    }

    private void handleDisplayNameEdited() {
        if (updatingDisplayName) {
            return;
        }
        String current = trimToNull(displayNameField.getText());
        if (current == null) {
            displayNameCustomized = false;
            updateDisplayNameSuggestion();
            return;
        }
        displayNameCustomized = !current.equals(trimToNull(lastSuggestedDisplayName));
    }

    private void updateDisplayNameSuggestion() {
        String suggestion = suggestedDisplayName();
        String current = trimToNull(displayNameField.getText());
        boolean shouldUpdate = !displayNameCustomized || current == null || current.equals(trimToNull(lastSuggestedDisplayName));
        lastSuggestedDisplayName = suggestion;
        if (shouldUpdate) {
            setDisplayNameText(suggestion);
            displayNameCustomized = false;
        }
    }

    private void setDisplayNameText(String value) {
        updatingDisplayName = true;
        try {
            displayNameField.setText(nullToEmpty(value));
        } finally {
            updatingDisplayName = false;
        }
    }

    private boolean isCustomDisplayName() {
        String current = trimToNull(displayNameField.getText());
        return current != null && !current.equals(trimToNull(lastSuggestedDisplayName));
    }

    private String suggestedDisplayName() {
        PersistenceKind selectedKind = (PersistenceKind) kindCombo.getSelectedItem();
        if (selectedKind == null) {
            selectedKind = currentKind != null ? currentKind : PersistenceKind.SQLITE;
        }
        return PersistenceProfile.suggestedDisplayName(
                selectedKind,
                databaseField.getText(),
                partTableField.getText(),
                persistenceNameField.getText()
        );
    }

    private void saveAndClose() {
        try {
            value = draftProfile().withId(value.id());
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid Profile", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PersistenceProfile draftProfile() {
        PersistenceKind kind = (PersistenceKind) kindCombo.getSelectedItem();
        Integer port = portField.getText().isBlank() ? null : Integer.parseInt(portField.getText().trim());
        PersistenceProfile profile = new PersistenceProfile(
                null,
                displayNameField.getText(),
                kind,
                keyClassField.getText(),
                persistenceNameField.getText(),
                hostField.getText(),
                port,
                databaseField.getText(),
                schemaField.getText(),
                partTableField.getText(),
                completedLogTableField.getText(),
                userField.getText(),
                new String(passwordField.getPassword()),
                redisUriField.getText(),
                (PayloadEncryptionMode) payloadEncryptionModeCombo.getSelectedItem(),
                new String(encryptionSecretField.getPassword())
        ).normalized();
        if (profile.kind() == PersistenceKind.DERBY && !profile.schema().matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Derby database name must start with a letter and use only letters, digits, or underscores.");
        }
        if (profile.displayName().isBlank()) {
            throw new IllegalArgumentException("Display name is required");
        }
        return profile;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable action;

        private SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }

    private record FormRow(JLabel label, Component field) {
        private void setVisible(boolean visible) {
            label.setVisible(visible);
            field.setVisible(visible);
        }
    }
}
