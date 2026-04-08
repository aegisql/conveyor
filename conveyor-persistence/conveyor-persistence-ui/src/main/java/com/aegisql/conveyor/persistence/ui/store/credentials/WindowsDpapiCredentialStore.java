package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

final class WindowsDpapiCredentialStore implements CredentialStore {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS windows_protected_credentials (
                profile_id INTEGER NOT NULL,
                secret_kind TEXT NOT NULL,
                protected_secret TEXT NOT NULL,
                PRIMARY KEY (profile_id, secret_kind)
            )
            """;

    private final Path databasePath;
    private final WindowsDataProtector dataProtector;

    WindowsDpapiCredentialStore(Path databasePath) {
        this(databasePath, new PowerShellWindowsDataProtector());
    }

    WindowsDpapiCredentialStore(Path databasePath, WindowsDataProtector dataProtector) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null").toAbsolutePath();
        this.dataProtector = Objects.requireNonNull(dataProtector, "dataProtector must not be null");
        ensureParentDirectory();
        ensureSchema();
    }

    static boolean isSupported() {
        return PowerShellWindowsDataProtector.isSupported();
    }

    @Override
    public String displayName() {
        return "Windows DPAPI";
    }

    @Override
    public Optional<String> load(long profileId, CredentialKind credentialKind) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT protected_secret
                     FROM windows_protected_credentials
                     WHERE profile_id = ? AND secret_kind = ?
                     """)) {
            statement.setLong(1, profileId);
            statement.setString(2, credentialKind.storageKey());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(dataProtector.unprotect(resultSet.getString("protected_secret")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load Windows-protected credential", e);
        }
    }

    @Override
    public void save(long profileId, CredentialKind credentialKind, String secret) {
        if (secret == null || secret.isBlank()) {
            delete(profileId, credentialKind);
            return;
        }
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO windows_protected_credentials (profile_id, secret_kind, protected_secret)
                     VALUES (?, ?, ?)
                     ON CONFLICT(profile_id, secret_kind)
                     DO UPDATE SET protected_secret = excluded.protected_secret
                     """)) {
            statement.setLong(1, profileId);
            statement.setString(2, credentialKind.storageKey());
            statement.setString(3, dataProtector.protect(secret));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save Windows-protected credential", e);
        }
    }

    @Override
    public void delete(long profileId, CredentialKind credentialKind) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM windows_protected_credentials WHERE profile_id = ? AND secret_kind = ?
                     """)) {
            statement.setLong(1, profileId);
            statement.setString(2, credentialKind.storageKey());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete Windows-protected credential", e);
        }
    }

    private void ensureParentDirectory() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create Windows credential-store directory for " + databasePath, e);
        }
    }

    private void ensureSchema() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize Windows DPAPI credential store", e);
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver is not available", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }
}
