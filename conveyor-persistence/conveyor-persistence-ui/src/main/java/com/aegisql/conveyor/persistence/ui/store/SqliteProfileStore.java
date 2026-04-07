package com.aegisql.conveyor.persistence.ui.store;

import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;

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
import java.util.List;
import java.util.Objects;

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
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private final Path databasePath;

    public SqliteProfileStore(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath must not be null").toAbsolutePath();
        ensureParentDirectory();
        ensureSchema();
    }

    public static Path defaultDatabasePath() {
        return Path.of(System.getProperty("user.home"), ".conveyor", "persistence-ui", "profiles.db");
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
            return insert(normalized);
        }
        update(normalized);
        return normalized;
    }

    public void delete(long id) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM profiles WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete persistence profile id=" + id, e);
        }
    }

    private PersistenceProfile insert(PersistenceProfile profile) {
        String sql = """
                INSERT INTO profiles (
                    display_name, kind, key_class_name, persistence_name, host, port,
                    database_name, schema_name, part_table, completed_log_table,
                    user_name, password, redis_uri
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    user_name = ?, password = ?, redis_uri = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindProfile(statement, profile);
            statement.setLong(14, profile.id());
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
        statement.setString(12, profile.password());
        statement.setString(13, profile.redisUri());
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
                resultSet.getString("password"),
                resultSet.getString("redis_uri")
        ).normalized();
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
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize SQLite profile store", e);
        }
    }
}
