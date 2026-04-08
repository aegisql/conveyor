package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.init.JdbcInitializationScriptOptions;
import com.aegisql.conveyor.persistence.ui.model.ConnectionStatus;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;
import com.aegisql.conveyor.persistence.ui.model.PersistenceSnapshot;
import com.aegisql.conveyor.persistence.ui.model.SummaryEntry;
import com.aegisql.conveyor.persistence.ui.model.TableData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;

final class JdbcPersistenceBackend implements PersistenceBackend {

    private static final String CART_VALUE_COLUMN = "CART_VALUE";
    private static final String VALUE_TYPE_COLUMN = "VALUE_TYPE";

    @Override
    public ConnectionStatus connectionStatus(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        if (requiresDatabaseSelection(normalized)) {
            return probeBootstrapConnectionStatus(normalized);
        }
        try (Connection connection = openConnection(normalized)) {
            executeHealthQuery(connection, normalized.kind());
            return connectionStatus(connection, normalized);
        } catch (Exception e) {
            if (canInitializeLocalDerby(normalized, e)) {
                return ConnectionStatus.CONNECTED_UNINITIALIZED;
            }
            PersistenceSnapshot fallback = inspectWithBootstrapFallback(normalized, e, 0);
            return fallback == null ? ConnectionStatus.FAILED : fallback.status();
        }
    }

    @Override
    public PersistenceSnapshot inspect(PersistenceProfile profile, int rowLimit, int pageIndex) {
        PersistenceProfile normalized = profile.normalized();
        int normalizedPageIndex = Math.max(0, pageIndex);
        if (requiresDatabaseSelection(normalized)) {
            return inspectWithoutSelectedDatabase(normalized, normalizedPageIndex);
        }
        try (Connection connection = openConnection(normalized)) {
            return inspectInitializedDatabase(connection, normalized, rowLimit, normalizedPageIndex);
        } catch (Exception e) {
            if (canInitializeLocalDerby(normalized, e)) {
                return localDerbyInitializationSnapshot(normalized, e, normalizedPageIndex);
            }
            PersistenceSnapshot fallback = inspectWithBootstrapFallback(normalized, e, normalizedPageIndex);
            if (fallback != null) {
                return fallback;
            }
            return new PersistenceSnapshot(
                    ConnectionStatus.FAILED,
                    "Connection failed",
                    e.getMessage(),
                    List.of(
                            new SummaryEntry("Engine", normalized.kind().displayName()),
                            new SummaryEntry("Target", normalized.database() != null ? normalized.database() : connectionUrlSafe(normalized))
                    ),
                    List.of(),
                    List.of(),
                    normalizedPageIndex,
                    normalizedPageIndex > 0,
                    false,
                    false,
                    true,
                    false,
                    false
            );
        }
    }

    @Override
    public List<String> lookupDatabases(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        if (!normalized.kind().isJdbc()) {
            return List.of();
        }
        try (Connection connection = openLookupConnectionForDatabases(normalized)) {
            TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getCatalogs()) {
                while (resultSet.next()) {
                    addLookupValue(values, resultSet.getString(1));
                }
            }
            if (values.isEmpty()) {
                addLookupValue(values, normalized.database());
            }
            return new ArrayList<>(values);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to lookup databases for " + normalized.kind().displayName(), e);
        }
    }

    @Override
    public List<String> lookupSchemas(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        if (!normalized.kind().isJdbc()) {
            return List.of();
        }
        try (Connection connection = openLookupConnectionForSchemas(normalized)) {
            TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet resultSet = metadata.getSchemas()) {
                while (resultSet.next()) {
                    addLookupValue(values, resultSet.getString("TABLE_SCHEM"));
                }
            }
            if (values.isEmpty()) {
                addLookupValue(values, normalized.schema());
            }
            return new ArrayList<>(values);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to lookup schemas for " + normalized.kind().displayName(), e);
        }
    }

    @Override
    public List<String> lookupPersistenceNames(PersistenceProfile profile) {
        return List.of();
    }

    @Override
    public String initializationScript(PersistenceProfile profile) {
        requirePersistenceDatabase(profile);
        return newBuilder(profile).initializationScript(new JdbcInitializationScriptOptions(true, null));
    }

    @Override
    public String initializationJavaCode(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        return JavaInitializationCodeGenerator.jdbcInitializationCode(normalized, effectiveDatabaseValue(normalized));
    }

    @Override
    public void initialize(PersistenceProfile profile) {
        requirePersistenceDatabase(profile);
        newBuilder(profile).autoInit(false).init();
    }

    @Override
    public void archiveExpired(PersistenceProfile profile) {
        requirePersistenceDatabase(profile);
        try (Persistence<Object> persistence = buildPersistence(profile)) {
            persistence.archiveExpiredParts();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive expired JDBC persistence data", e);
        }
    }

    @Override
    public void archiveAll(PersistenceProfile profile) {
        requirePersistenceDatabase(profile);
        try (Persistence<Object> persistence = buildPersistence(profile)) {
            persistence.archiveAll();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive all JDBC persistence data", e);
        }
    }

    private Persistence<Object> buildPersistence(PersistenceProfile profile) {
        try {
            @SuppressWarnings("unchecked")
            Persistence<Object> persistence = (Persistence<Object>) newBuilder(profile).autoInit(false).build();
            return persistence;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JDBC persistence", e);
        }
    }

    private JdbcPersistenceBuilder<?> newBuilder(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        if (!normalized.kind().isJdbc()) {
            throw new IllegalArgumentException("JDBC backend does not support kind " + normalized.kind());
        }
        try {
            Class<?> keyClass = Class.forName(normalized.keyClassName());
            JdbcPersistenceBuilder<?> builder = JdbcPersistenceBuilder.presetInitializer(normalized.kind().jdbcType(), keyClass)
                    .autoInit(false)
                    .partTable(normalized.partTable())
                    .completedLogTable(normalized.completedLogTable());
            if (normalized.host() != null) {
                builder = builder.host(normalized.host());
            }
            if (normalized.port() != null) {
                builder = builder.port(normalized.port());
            }
            String effectiveDatabase = effectiveDatabaseValue(normalized);
            if (effectiveDatabase != null) {
                builder = builder.database(effectiveDatabase);
            }
            if (normalized.schema() != null) {
                builder = builder.schema(normalized.schema());
            }
            if (normalized.user() != null) {
                builder = builder.user(normalized.user());
            }
            if (normalized.password() != null) {
                builder = builder.password(normalized.password());
            }
            return ProfileEncryptionSupport.apply(builder, normalized);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load key class " + normalized.keyClassName(), e);
        }
    }

    private Connection openConnection(PersistenceProfile profile) throws Exception {
        PersistenceProfile normalized = profile.normalized();
        Class.forName(driverClassName(normalized.kind()));
        return openConnection(connectionUrl(normalized), normalized.user(), normalized.password());
    }

    private Connection openLookupConnectionForDatabases(PersistenceProfile profile) throws Exception {
        if (profile.kind().isNetwork() && supportsBootstrapConnectionWithoutDatabase(profile.kind())) {
            return openBootstrapConnection(profile);
        }
        return openConnection(profile);
    }

    private Connection openLookupConnectionForSchemas(PersistenceProfile profile) throws Exception {
        if (profile.database() == null && supportsBootstrapConnectionWithoutDatabase(profile.kind())) {
            return openBootstrapConnection(profile);
        }
        return openConnection(profile);
    }

    private String connectionUrlSafe(PersistenceProfile profile) {
        try {
            return connectionUrl(profile);
        } catch (Exception e) {
            return profile.kind().displayName();
        }
    }

    private String connectionUrl(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        return switch (normalized.kind()) {
            case MYSQL -> jdbcUrlWithOptionalDatabase("jdbc:mysql://", normalized);
            case MARIADB -> jdbcUrlWithOptionalDatabase("jdbc:mariadb://", normalized);
            case POSTGRES -> {
                requirePersistenceDatabase(normalized);
                String base = "jdbc:postgresql://" + normalized.host() + ':' + normalized.port() + '/' + normalized.database();
                yield normalized.schema() == null ? base : base + "?currentSchema=" + normalized.schema();
            }
            case ORACLE -> {
                requirePersistenceDatabase(normalized);
                yield "jdbc:oracle:thin:@//" + normalized.host() + ':' + normalized.port() + '/' + normalized.database();
            }
            case SQLSERVER -> {
                requirePersistenceDatabase(normalized);
                yield "jdbc:sqlserver://" + normalized.host() + ':' + normalized.port()
                        + ";databaseName=" + normalized.database() + ";encrypt=false;trustServerCertificate=true";
            }
            case SQLITE -> "jdbc:sqlite:" + normalized.database();
            case DERBY -> "jdbc:derby:" + effectiveDatabaseValue(normalized) + ';';
            case REDIS -> throw new IllegalArgumentException("Redis does not use JDBC URLs");
        };
    }

    private String jdbcUrlWithOptionalDatabase(String prefix, PersistenceProfile profile) {
        StringBuilder url = new StringBuilder(prefix)
                .append(profile.host())
                .append(':')
                .append(profile.port());
        if (profile.database() != null) {
            url.append('/').append(profile.database());
        }
        return url.toString();
    }

    private String driverClassName(PersistenceKind kind) {
        return switch (kind) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRES -> "org.postgresql.Driver";
            case ORACLE -> "oracle.jdbc.OracleDriver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case SQLITE -> "org.sqlite.JDBC";
            case DERBY -> "org.apache.derby.jdbc.EmbeddedDriver";
            case REDIS -> throw new IllegalArgumentException("Redis does not use JDBC drivers");
        };
    }

    private void executeHealthQuery(Connection connection, PersistenceKind kind) throws SQLException {
        String sql = switch (kind) {
            case ORACLE -> "SELECT 1 FROM DUAL";
            case DERBY -> "VALUES 1";
            default -> "SELECT 1";
        };
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Health check returned no rows");
            }
        }
    }

    private void addLookupValue(TreeSet<String> values, String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
    }

    private boolean tableExists(Connection connection, PersistenceProfile profile, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> names = List.of(tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT));
        List<Lookup> lookups = List.of(
                new Lookup(profile.database(), profile.schema()),
                new Lookup(profile.database(), null),
                new Lookup(null, profile.schema()),
                new Lookup(null, null)
        );
        for (Lookup lookup : lookups) {
            for (String name : names) {
                try (ResultSet resultSet = metadata.getTables(lookup.catalog(), lookup.schema(), name, new String[]{"TABLE"})) {
                    while (resultSet.next()) {
                        String found = resultSet.getString("TABLE_NAME");
                        if (tableName.equalsIgnoreCase(found)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private long countRows(Connection connection, String tableRef) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableRef);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private TablePreview previewTable(Connection connection, PersistenceProfile profile, String title, String tableRef, int rowLimit, int pageIndex) throws SQLException {
        int offset = Math.max(0, pageIndex) * Math.max(1, rowLimit);
        try (Statement statement = connection.createStatement()) {
            statement.setMaxRows(offset + Math.max(1, rowLimit) + 1);
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableRef)) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    columns.add(metaData.getColumnLabel(i));
                }
                int valueTypeIndex = indexOfColumn(columns, VALUE_TYPE_COLUMN);
                int cartValueIndex = indexOfColumn(columns, CART_VALUE_COLUMN);
                List<List<String>> rows = new ArrayList<>();
                boolean hasNext = false;
                int skipped = 0;
                while (resultSet.next()) {
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }
                    if (rows.size() >= rowLimit) {
                        hasNext = true;
                        break;
                    }
                    List<String> row = new ArrayList<>();
                    String valueType = valueTypeIndex >= 0 ? resultSet.getString(valueTypeIndex + 1) : null;
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        if (i == cartValueIndex + 1 && valueType != null) {
                            row.add(formatCartValue(profile, resultSet, i, valueType));
                        } else {
                            row.add(formatJdbcValue(resultSet.getObject(i)));
                        }
                    }
                    rows.add(row);
                }
                return new TablePreview(new TableData(title, columns, rows), hasNext);
            }
        }
    }

    private int indexOfColumn(List<String> columns, String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columnName.equalsIgnoreCase(columns.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String formatCartValue(PersistenceProfile profile, ResultSet resultSet, int valueIndex, String valueType) throws SQLException {
        byte[] bytes = resultSet.getBytes(valueIndex);
        if (bytes == null) {
            return "";
        }
        try {
            ConverterAdviser<Object> converterAdviser = ProfileEncryptionSupport.isEncryptedHint(valueType)
                    ? ProfileEncryptionSupport.newDecryptingConverterAdviser(profile)
                    : new ConverterAdviser<>();
            Object decoded = converterAdviser.getConverter(null, valueType).fromPersistence(bytes);
            return formatJdbcValue(decoded);
        } catch (Exception ignored) {
            if (ProfileEncryptionSupport.isEncryptedHint(valueType)) {
                return ProfileEncryptionSupport.encryptedPayloadMessage(profile);
            }
            return formatJdbcValue(resultSet.getObject(valueIndex));
        }
    }

    private String qualifiedTableName(PersistenceProfile profile, String tableName) {
        Objects.requireNonNull(tableName, "tableName must not be null");
        if (profile.kind().supportsSchemaQualifier() && profile.schema() != null) {
            return profile.schema() + '.' + tableName;
        }
        return tableName;
    }

    private void requirePersistenceDatabase(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        if (normalized.kind().isJdbc() && normalized.kind().isNetwork() && normalized.database() == null) {
            throw new IllegalArgumentException("Database name is required for " + normalized.kind().displayName() + " persistence actions");
        }
    }

    private ConnectionStatus connectionStatus(Connection connection, PersistenceProfile profile) throws SQLException {
        boolean partExists = tableExists(connection, profile, profile.partTable());
        boolean completedExists = tableExists(connection, profile, profile.completedLogTable());
        return partExists && completedExists ? ConnectionStatus.READY : ConnectionStatus.CONNECTED_UNINITIALIZED;
    }

    private PersistenceSnapshot inspectInitializedDatabase(Connection connection, PersistenceProfile normalized, int rowLimit, int pageIndex) throws SQLException {
        boolean partExists = tableExists(connection, normalized, normalized.partTable());
        boolean completedExists = tableExists(connection, normalized, normalized.completedLogTable());
        List<SummaryEntry> summary = new ArrayList<>();
        summary.add(new SummaryEntry("Engine", normalized.kind().displayName()));
        summary.add(new SummaryEntry("URL", connectionUrl(normalized)));
        summary.add(new SummaryEntry("Part Table", normalized.partTable()));
        summary.add(new SummaryEntry("Completed Log Table", normalized.completedLogTable()));
        summary.add(new SummaryEntry("Part Table Present", Boolean.toString(partExists)));
        summary.add(new SummaryEntry("Completed Log Present", Boolean.toString(completedExists)));
        summary.add(new SummaryEntry("Preview Page", Integer.toString(pageIndex + 1)));

        if (!partExists || !completedExists) {
            return new PersistenceSnapshot(
                    ConnectionStatus.CONNECTED_UNINITIALIZED,
                    "Connected, persistence not initialized",
                    "The database connection works, but one or both persistence tables are missing.",
                    summary,
                    List.of(),
                    indexInfoTables(connection, normalized),
                    pageIndex,
                    pageIndex > 0,
                    false,
                    true,
                    true,
                    false,
                    false
            );
        }

        String partTableRef = qualifiedTableName(normalized, normalized.partTable());
        String completedTableRef = qualifiedTableName(normalized, normalized.completedLogTable());
        long partCount = countRows(connection, partTableRef);
        long completedCount = countRows(connection, completedTableRef);
        summary.add(new SummaryEntry("Stored Parts", Long.toString(partCount)));
        summary.add(new SummaryEntry("Completed Keys", Long.toString(completedCount)));
        summary.add(new SummaryEntry("Preview Limit", Integer.toString(rowLimit)));
        TablePreview partPreview = previewTable(connection, normalized, "Parts", partTableRef, rowLimit, pageIndex);
        TablePreview completedPreview = previewTable(connection, normalized, "Completed Keys", completedTableRef, rowLimit, pageIndex);

        return new PersistenceSnapshot(
                ConnectionStatus.READY,
                "Connected and initialized",
                "Showing raw table previews for the configured JDBC persistence.",
                summary,
                List.of(
                        partPreview.table(),
                        completedPreview.table()
                ),
                indexInfoTables(connection, normalized),
                pageIndex,
                pageIndex > 0,
                partPreview.hasNext() || completedPreview.hasNext(),
                false,
                true,
                true,
                true
        );
    }

    private PersistenceSnapshot inspectWithoutSelectedDatabase(PersistenceProfile profile, int pageIndex) {
        if (!supportsBootstrapConnectionWithoutDatabase(profile.kind())) {
            return new PersistenceSnapshot(
                    ConnectionStatus.FAILED,
                    "Database name is required",
                    "Choose the target database or service name before connecting to " + profile.kind().displayName() + '.',
                    List.of(
                            new SummaryEntry("Engine", profile.kind().displayName()),
                            new SummaryEntry("Target Database", "<not selected>")
                    ),
                    List.of(),
                    List.of(),
                    pageIndex,
                    pageIndex > 0,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
        try (Connection ignored = openBootstrapConnection(profile)) {
            return new PersistenceSnapshot(
                    ConnectionStatus.CONNECTED_UNINITIALIZED,
                    "Connected, database not selected",
                    "The server is reachable. Choose the target database name before generating initialization SQL or creating persistence tables.",
                    List.of(
                            new SummaryEntry("Engine", profile.kind().displayName()),
                            new SummaryEntry("Server URL", bootstrapConnectionUrl(profile)),
                            new SummaryEntry("Target Database", "<not selected>")
                    ),
                    List.of(),
                    List.of(),
                    pageIndex,
                    pageIndex > 0,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        } catch (Exception e) {
            return new PersistenceSnapshot(
                    ConnectionStatus.FAILED,
                    "Connection failed",
                    e.getMessage(),
                    List.of(
                            new SummaryEntry("Engine", profile.kind().displayName()),
                            new SummaryEntry("Target", connectionUrlSafe(profile))
                    ),
                    List.of(),
                    List.of(),
                    pageIndex,
                    pageIndex > 0,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }

    private ConnectionStatus probeBootstrapConnectionStatus(PersistenceProfile profile) {
        if (!supportsBootstrapConnectionWithoutDatabase(profile.kind())) {
            return ConnectionStatus.FAILED;
        }
        try (Connection connection = openBootstrapConnection(profile)) {
            executeHealthQuery(connection, profile.kind());
            return ConnectionStatus.CONNECTED_UNINITIALIZED;
        } catch (Exception e) {
            return ConnectionStatus.FAILED;
        }
    }

    private PersistenceSnapshot inspectWithBootstrapFallback(PersistenceProfile profile, Exception originalFailure, int pageIndex) {
        if (!supportsBootstrapConnectionWithoutDatabase(profile.kind()) || profile.database() == null) {
            return null;
        }
        try (Connection ignored = openBootstrapConnection(profile)) {
            return new PersistenceSnapshot(
                    ConnectionStatus.CONNECTED_UNINITIALIZED,
                    "Connected, target database not initialized",
                    "The server is reachable, but the configured database '" + profile.database()
                            + "' is not available yet. You can initialize persistence to create it and then continue in that database.",
                    List.of(
                            new SummaryEntry("Engine", profile.kind().displayName()),
                            new SummaryEntry("Server URL", bootstrapConnectionUrl(profile)),
                            new SummaryEntry("Target Database", profile.database()),
                            new SummaryEntry("Connection Error", originalFailure.getMessage())
                    ),
                    List.of(),
                    List.of(),
                    pageIndex,
                    pageIndex > 0,
                    false,
                    true,
                    true,
                    false,
                    false
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private Connection openBootstrapConnection(PersistenceProfile profile) throws Exception {
        Class.forName(driverClassName(profile.kind()));
        return openConnection(bootstrapConnectionUrl(profile), profile.user(), profile.password());
    }

    private Connection openConnection(String url, String user, String password) throws SQLException {
        if (user != null) {
            return DriverManager.getConnection(url, user, password == null ? "" : password);
        }
        if (password != null) {
            Properties properties = new Properties();
            properties.put("password", password);
            return DriverManager.getConnection(url, properties);
        }
        return DriverManager.getConnection(url);
    }

    private String bootstrapConnectionUrl(PersistenceProfile profile) {
        return switch (profile.kind()) {
            case MYSQL -> "jdbc:mysql://" + profile.host() + ':' + profile.port() + '/';
            case MARIADB -> "jdbc:mariadb://" + profile.host() + ':' + profile.port() + '/';
            case POSTGRES -> "jdbc:postgresql://" + profile.host() + ':' + profile.port() + '/';
            case SQLSERVER -> "jdbc:sqlserver://" + profile.host() + ':' + profile.port() + ";encrypt=false;trustServerCertificate=true";
            default -> connectionUrl(profile);
        };
    }

    private boolean supportsBootstrapConnectionWithoutDatabase(PersistenceKind kind) {
        return switch (kind) {
            case MYSQL, MARIADB, POSTGRES, SQLSERVER -> true;
            default -> false;
        };
    }

    private boolean requiresDatabaseSelection(PersistenceProfile profile) {
        return profile.kind().isJdbc()
                && profile.kind().isNetwork()
                && profile.database() == null;
    }

    private String effectiveDatabaseValue(PersistenceProfile profile) {
        if (profile.database() == null || profile.kind() != PersistenceKind.DERBY) {
            return profile.database();
        }
        Path configured = Path.of(profile.database());
        if (Files.isDirectory(configured) && !looksLikeDerbyDatabaseDirectory(configured)) {
            String databaseName = profile.schema() != null ? profile.schema() : "conveyor_db";
            return configured.resolve(databaseName).toString();
        }
        return profile.database();
    }

    private boolean looksLikeDerbyDatabaseDirectory(Path path) {
        return Files.exists(path.resolve("service.properties"));
    }

    private boolean canInitializeLocalDerby(PersistenceProfile profile, Exception error) {
        if (profile.kind() != PersistenceKind.DERBY || profile.database() == null) {
            return false;
        }
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("database")
                && (lower.contains("not found")
                || lower.contains("does not exist")
                || lower.contains("not created"));
    }

    private PersistenceSnapshot localDerbyInitializationSnapshot(PersistenceProfile profile, Exception originalFailure, int pageIndex) {
        return new PersistenceSnapshot(
                ConnectionStatus.CONNECTED_UNINITIALIZED,
                "Derby database not initialized",
                "The selected Derby home directory is reachable but the target database has not been created yet. Use Initialize to create the database and persistence tables.",
                List.of(
                        new SummaryEntry("Engine", profile.kind().displayName()),
                        new SummaryEntry("Database Home Directory", profile.database()),
                        new SummaryEntry("Target Database Path", effectiveDatabaseValue(profile)),
                        new SummaryEntry("Connection Error", originalFailure.getMessage())
                ),
                List.of(),
                List.of(),
                pageIndex,
                pageIndex > 0,
                false,
                true,
                true,
                false,
                false
        );
    }

    private String formatJdbcValue(Object value) {
        if (value == null) {
            return "";
        }
        try {
            if (value instanceof byte[] bytes) {
                return "<" + bytes.length + " bytes>";
            }
            if (value instanceof Blob blob) {
                return "<blob " + blob.length() + " bytes>";
            }
            if (value instanceof Clob clob) {
                return "<clob " + clob.length() + " chars>";
            }
        } catch (SQLException e) {
            return "<unavailable>";
        }
        String stringValue = String.valueOf(value);
        return stringValue.length() > 160 ? stringValue.substring(0, 157) + "..." : stringValue;
    }

    private List<TableData> indexInfoTables(Connection connection, PersistenceProfile profile) {
        try {
            TableData indexTable = indexInfoTable(connection, profile);
            return indexTable.rows().isEmpty() ? List.of() : List.of(indexTable);
        } catch (SQLException | UnsupportedOperationException e) {
            return List.of();
        }
    }

    private TableData indexInfoTable(Connection connection, PersistenceProfile profile) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        Map<String, IndexDetails> indexes = new LinkedHashMap<>();
        collectIndexes(metadata, profile, profile.partTable(), indexes);
        collectIndexes(metadata, profile, profile.completedLogTable(), indexes);
        List<List<String>> rows = indexes.values().stream()
                .map(index -> List.of(index.tableName, index.indexName, Boolean.toString(index.unique), String.join(", ", index.columns)))
                .toList();
        return new TableData("Indexes", List.of("Table", "Index", "Unique", "Columns"), rows);
    }

    private void collectIndexes(DatabaseMetaData metadata, PersistenceProfile profile, String tableName, Map<String, IndexDetails> indexes) throws SQLException {
        List<String> names = List.of(tableName, tableName.toUpperCase(Locale.ROOT), tableName.toLowerCase(Locale.ROOT));
        List<Lookup> lookups = List.of(
                new Lookup(profile.database(), profile.schema()),
                new Lookup(profile.database(), null),
                new Lookup(null, profile.schema()),
                new Lookup(null, null)
        );
        for (Lookup lookup : lookups) {
            for (String name : names) {
                try (ResultSet resultSet = metadata.getIndexInfo(lookup.catalog(), lookup.schema(), name, false, false)) {
                    while (resultSet.next()) {
                        short type = resultSet.getShort("TYPE");
                        if (type == DatabaseMetaData.tableIndexStatistic) {
                            continue;
                        }
                        String indexName = resultSet.getString("INDEX_NAME");
                        String actualTable = resultSet.getString("TABLE_NAME");
                        String columnName = resultSet.getString("COLUMN_NAME");
                        if (indexName == null || actualTable == null || columnName == null) {
                            continue;
                        }
                        boolean unique = !resultSet.getBoolean("NON_UNIQUE");
                        String key = actualTable.toLowerCase(Locale.ROOT) + '|' + indexName.toLowerCase(Locale.ROOT);
                        indexes.computeIfAbsent(key, ignored -> new IndexDetails(actualTable, indexName, unique))
                                .addColumn(columnName);
                    }
                } catch (SQLFeatureNotSupportedException ignored) {
                    return;
                }
            }
        }
    }

    private record Lookup(String catalog, String schema) {
    }

    private record TablePreview(TableData table, boolean hasNext) {
    }

    private static final class IndexDetails {
        private final String tableName;
        private final String indexName;
        private final boolean unique;
        private final List<String> columns = new ArrayList<>();

        private IndexDetails(String tableName, String indexName, boolean unique) {
            this.tableName = tableName;
            this.indexName = indexName;
            this.unique = unique;
        }

        private IndexDetails addColumn(String column) {
            if (!columns.contains(column)) {
                columns.add(column);
            }
            return this;
        }
    }
}
