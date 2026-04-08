package com.aegisql.conveyor.persistence.ui.store.credentials;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.GridLayout;
import java.util.Arrays;

final class SwingMasterPasswordProvider implements MasterPasswordProvider {

    @Override
    public char[] createMasterPassword() {
        while (true) {
            JPasswordField passwordField = new JPasswordField(24);
            JPasswordField confirmField = new JPasswordField(24);
            JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
            panel.add(new JLabel("Create a master password for stored credentials"));
            panel.add(passwordField);
            panel.add(new JLabel("Confirm master password"));
            panel.add(confirmField);
            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Create Master Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) {
                clear(passwordField, confirmField);
                return null;
            }
            char[] password = passwordField.getPassword();
            char[] confirmation = confirmField.getPassword();
            clear(passwordField, confirmField);
            if (password.length == 0) {
                showError("Master password cannot be empty.");
                Arrays.fill(password, '\0');
                Arrays.fill(confirmation, '\0');
                continue;
            }
            if (!Arrays.equals(password, confirmation)) {
                showError("Master password values do not match.");
                Arrays.fill(password, '\0');
                Arrays.fill(confirmation, '\0');
                continue;
            }
            Arrays.fill(confirmation, '\0');
            return password;
        }
    }

    @Override
    public char[] requestMasterPassword() {
        JPasswordField passwordField = new JPasswordField(24);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(new JLabel("Enter the master password for stored credentials"));
        panel.add(passwordField);
        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Unlock Credentials",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            clear(passwordField);
            return null;
        }
        char[] password = passwordField.getPassword();
        clear(passwordField);
        return password;
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Credential Access Failed", JOptionPane.ERROR_MESSAGE);
    }

    private void clear(JPasswordField... fields) {
        for (JPasswordField field : fields) {
            char[] value = field.getPassword();
            Arrays.fill(value, '\0');
            field.setText("");
        }
    }
}
