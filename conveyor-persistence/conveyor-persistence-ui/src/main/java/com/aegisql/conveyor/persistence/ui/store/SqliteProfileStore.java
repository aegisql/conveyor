package com.aegisql.conveyor.persistence.ui.store;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.store.credentials.CredentialKind;
import com.aegisql.conveyor.persistence.ui.store.credentials.CredentialStore;
import com.aegisql.conveyor.persistence.ui.store.credentials.CredentialStores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SqliteProfileStore {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                display_name TEXT NOT NULL,
                kind TEXT NOT NULL,
                key_class_name TEXT,
                persistence_name TEXT,
                host TEXT,
                port INTEGER,
                database_name TEXT,
                schema_name TEXT,
                part_table TEXT,
                completed_log_table TEXT,
                user_name TEXT,
                password TEXT,
                redis_uri TEXT,
                payload_encryption_mode TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String CREATE_COLUMN_VISIBILITY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS hidden_columns (
                profile_id INTEGER NOT NULL,
                table_title TEXT NOT NULL,
                column_name TEXT NOT NULL,
                PRIMARY KEY (profile_id, table_title, column_name)
            )
            """;

    private final Path databasePath;
    private final CredentialStore credentialStore;

    public SqliteProfileStore(Path databasePath) {
        this(databasePath, CredentialStores.defaultStore(databasePath));
    }

    public SqliteProfileStore(Path databasePath, CredentialStore credentialStore) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null").toAbsolutePath();
        this.credentialStore = Objects.requireNonNull(credentialStore, "credentialStore must not be null");
        ensureParentDirectory();
        ensureSchema();
    }

    public static Path defaultDatabasePath() {
        return Path.of(System.getProperty("user.home"), ".conveyor", "persistence-ui", "profiles.db");
    }

    public String credentialStoreDisplayName() {
        return credentialStore.displayName();
    }

    public List<PersistenceProfile> listProfiles() {
        List<PersistenceProfile> profiles = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM profiles ORDER BY display_name, id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                profiles.add(map(resultSet));
            }
            return profiles;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list persistence profiles", e);
        }
    }

    public PersistenceProfile save(PersistenceProfile profile) {
        PersistenceProfile normalized = Objects.requireNonNull(profile, "profile must not be null").normalized();
        if (normalized.id() == null) {
            PersistenceProfile inserted = insert(normalized.withPassword(null).withEncryptionSecret(null));
            try {
                syncCredential(inserted.id(), CredentialKind.PASSWORD, normalized.password());
                syncCredential(inserted.id(), CredentialKind.ENCRYPTION_SECRET, normalized.encryptionSecret());
                return inserted.withPassword(normalized.password()).withEncryptionSecret(normalized.encryptionSecret());
            } catch (RuntimeException e) {
                deleteInsertedProfile(inserted.id());
                throw e;
            }
        }
        update(normalized.withPassword(null).withEncryptionSecret(null));
        syncCredential(normalized.id(), CredentialKind.PASSWORD, normalized.password());
        syncCredential(normalized.id(), CredentialKind.ENCRYPTION_SECRET, normalized.encryptionSecret());
        return normalized;
    }

    public void delete(long id) {
        try (Connection connection = openConnection();
             PreparedStatement deleteHiddenColumns = connection.prepareStatement("DELETE FROM hidden_columns WHERE profile_id = ?");
             PreparedStatement deleteProfile = connection.prepareStatement("DELETE FROM profiles WHERE id = ?")) {
            deleteHiddenColumns.setLong(1, id);
            deleteHiddenColumns.executeUpdate();
            deleteProfile.setLong(1, id);
            deleteProfile.executeUpdate();
            try {
                credentialStore.delete(id, CredentialKind.PASSWORD);
                credentialStore.delete(id, CredentialKind.ENCRYPTION_SECRET);
            } catch (RuntimeException ignored) {
                // Best-effort credential cleanup; stale secrets are less harmful than blocking profile deletion.
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete persistence profile id=" + id, e);
        }
    }

    public PersistenceProfile withCredentials(PersistenceProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (profile.id() == null) {
            return profile;
        }
        return profile.withPassword(loadCredential(profile.id(), CredentialKind.PASSWORD))
                .withEncryptionSecret(loadCredential(profile.id(), CredentialKind.ENCRYPTION_SECRET))
                .normalized();
    }

    public Map<String, Set<String>> loadHiddenColumns(long profileId) {
        Map<String, Set<String>> hiddenColumns = new HashMap<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT table_title, column_name
                     FROM hidden_columns
                     WHERE profile_id = ?
                     ORDER BY table_title, column_name
                     """)) {
            statement.setLong(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String tableTitle = resultSet.getString("table_title");
                    String columnName = resultSet.getString("column_name");
                    hiddenColumns.computeIfAbsent(tableTitle, ignored -> new HashSet<>()).add(columnName);
                }
            }
            return hiddenColumns;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load hidden columns for persistence profile id=" + profileId, e);
        }
    }

    public void saveHiddenColumns(long profileId, String tableTitle, Set<String> hiddenColumns) {
        Objects.requireNonNull(tableTitle, "tableTitle must not be null");
        Set<String> normalizedHiddenColumns = hiddenColumns == null ? Set.of() : Set.copyOf(hiddenColumns);
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement("""
                    DELETE FROM hidden_columns WHERE profile_id = ? AND table_title = ?
                    """);
                 PreparedStatement insertStatement = connection.prepareStatement("""
                    INSERT INTO hidden_columns (profile_id, table_title, column_name) VALUES (?, ?, ?)
                    """)) {
                deleteStatement.setLong(1, profileId);
                deleteStatement.setString(2, tableTitle);
                deleteStatement.executeUpdate();
                for (String columnName : normalizedHiddenColumns) {
                    insertStatement.setLong(1, profileId);
                    insertStatement.setString(2, tableTitle);
                    insertStatement.setString(3, columnName);
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save hidden columns for persistence profile id=" + profileId, e);
        }
    }

    private PersistenceProfile insert(PersistenceProfile profile) {
        String sql = """
                INSERT INTO profiles (
                    display_name, kind, key_class_name, persistence_name, host, port,
                    database_name, schema_name, part_table, completed_log_table,
                    user_name, password, redis_uri, payload_encryption_mode
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindProfile(statement, profile);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return profile.withId(keys.getLong(1));
                }
            }
            throw new IllegalStateException("SQLite profile insert did not return a generated id");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert persistence profile", e);
        }
    }

    private void update(PersistenceProfile profile) {
        String sql = """
                UPDATE profiles
                SET display_name = ?, kind = ?, key_class_name = ?, persistence_name = ?, host = ?, port = ?,
                    database_name = ?, schema_name = ?, part_table = ?, completed_log_table = ?,
                    user_name = ?, password = ?, redis_uri = ?, payload_encryption_mode = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProfile(statement, profile);
            statement.setLong(15, profile.id());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update persistence profile id=" + profile.id(), e);
        }
    }

    private void bindProfile(PreparedStatement statement, PersistenceProfile profile) throws SQLException {
        statement.setString(1, profile.displayName());
        statement.setString(2, profile.kind().name());
        statement.setString(3, profile.keyClassName());
        statement.setString(4, profile.persistenceName());
        statement.setString(5, profile.host());
        if (profile.port() == null) {
            statement.setNull(6, java.sql.Types.INTEGER);
        } else {
            statement.setInt(6, profile.port());
        }
        statement.setString(7, profile.database());
        statement.setString(8, profile.schema());
        statement.setString(9, profile.partTable());
        statement.setString(10, profile.completedLogTable());
        statement.setString(11, profile.user());
        statement.setString(12, null);
        statement.setString(13, profile.redisUri());
        statement.setString(14, profile.payloadEncryptionMode().name());
    }

    private PersistenceProfile map(ResultSet resultSet) throws SQLException {
        int rawPort = resultSet.getInt("port");
        Integer portValue = resultSet.wasNull() ? null : rawPort;
        return new PersistenceProfile(
                resultSet.getLong("id"),
                resultSet.getString("display_name"),
                PersistenceKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("key_class_name"),
                resultSet.getString("persistence_name"),
                resultSet.getString("host"),
                portValue,
                resultSet.getString("database_name"),
                resultSet.getString("schema_name"),
                resultSet.getString("part_table"),
                resultSet.getString("completed_log_table"),
                resultSet.getString("user_name"),
                null,
                resultSet.getString("redis_uri"),
                mapEncryptionMode(resultSet.getString("payload_encryption_mode")),
                null
        ).normalized();
    }

    private void syncCredential(long profileId, CredentialKind credentialKind, String secret) {
        if (secret == null || secret.isBlank()) {
            credentialStore.delete(profileId, credentialKind);
            if (credentialKind == CredentialKind.PASSWORD) {
                clearLegacyPassword(profileId);
            }
            return;
        }
        credentialStore.save(profileId, credentialKind, secret);
        if (credentialKind == CredentialKind.PASSWORD) {
            clearLegacyPassword(profileId);
        }
    }

    private String loadCredential(long profileId, CredentialKind credentialKind) {
        if (credentialKind == CredentialKind.PASSWORD) {
            return credentialStore.load(profileId, credentialKind)
                    .orElseGet(() -> migrateLegacyPassword(profileId));
        }
        return credentialStore.load(profileId, credentialKind).orElse(null);
    }

    private String migrateLegacyPassword(long profileId) {
        String legacyPassword = loadLegacyPassword(profileId);
        if (legacyPassword == null || legacyPassword.isBlank()) {
            return null;
        }
        credentialStore.save(profileId, CredentialKind.PASSWORD, legacyPassword);
        clearLegacyPassword(profileId);
        return legacyPassword;
    }

    private String loadLegacyPassword(long profileId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT password FROM profiles WHERE id = ?")) {
            statement.setLong(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getString("password");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load legacy password for persistence profile id=" + profileId, e);
        }
    }

    private void clearLegacyPassword(long profileId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE profiles SET password = NULL WHERE id = ?")) {
            statement.setLong(1, profileId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear legacy password for persistence profile id=" + profileId, e);
        }
    }

    private void deleteInsertedProfile(long profileId) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM profiles WHERE id = ?")) {
            statement.setLong(1, profileId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Best-effort rollback for insert + credential failures.
        }
        try {
            credentialStore.delete(profileId, CredentialKind.PASSWORD);
            credentialStore.delete(profileId, CredentialKind.ENCRYPTION_SECRET);
        } catch (RuntimeException ignored) {
            // Best-effort rollback for insert + credential failures.
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

    private void ensureParentDirectory() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create profile-store directory for " + databasePath, e);
        }
    }

    private void ensureSchema() {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TABLE_SQL);
            statement.executeUpdate(CREATE_COLUMN_VISIBILITY_TABLE_SQL);
            ensureColumnExists(connection, "profiles", "payload_encryption_mode", "TEXT");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite profile store", e);
        }
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ')');
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement alter = connection.createStatement()) {
            alter.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + ' ' + definition);
        }
    }

    private PayloadEncryptionMode mapEncryptionMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PayloadEncryptionMode.NONE;
        }
        try {
            return PayloadEncryptionMode.valueOf(rawValue);
        } catch (IllegalArgumentException ignored) {
            return PayloadEncryptionMode.NONE;
        }
    }
}
