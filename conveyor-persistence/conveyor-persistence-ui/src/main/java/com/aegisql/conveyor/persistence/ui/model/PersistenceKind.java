package com.aegisql.conveyor.persistence.ui.model;

public enum PersistenceKind {
    MYSQL("MySQL", "mysql", true, false, true, false, null, null, 3306),
    MARIADB("MariaDB", "mariadb", true, false, true, false, null, null, 3306),
    POSTGRES("PostgreSQL", "postgres", true, false, true, true, null, null, 5432),
    ORACLE("Oracle", "oracle", true, false, true, true, null, null, 1521),
    SQLSERVER("SQL Server", "sqlserver", true, false, true, true, null, null, 1433),
    SQLITE("SQLite", "sqlite", true, false, false, false, "conveyor.db", null, null),
    DERBY("Derby Embedded", "derby", true, false, false, false, null, "conveyor_db", null),
    REDIS("Redis", null, false, true, false, false, null, null, 6379);

    private final String displayName;
    private final String jdbcType;
    private final boolean jdbc;
    private final boolean redis;
    private final boolean network;
    private final boolean schemaQualifier;
    private final String defaultDatabase;
    private final String defaultSchema;
    private final Integer defaultPort;

    PersistenceKind(
            String displayName,
            String jdbcType,
            boolean jdbc,
            boolean redis,
            boolean network,
            boolean schemaQualifier,
            String defaultDatabase,
            String defaultSchema,
            Integer defaultPort
    ) {
        this.displayName = displayName;
        this.jdbcType = jdbcType;
        this.jdbc = jdbc;
        this.redis = redis;
        this.network = network;
        this.schemaQualifier = schemaQualifier;
        this.defaultDatabase = defaultDatabase;
        this.defaultSchema = defaultSchema;
        this.defaultPort = defaultPort;
    }

    public String displayName() {
        return displayName;
    }

    public String jdbcType() {
        return jdbcType;
    }

    public boolean isJdbc() {
        return jdbc;
    }

    public boolean isRedis() {
        return redis;
    }

    public boolean isNetwork() {
        return network;
    }

    public boolean supportsSchemaQualifier() {
        return schemaQualifier;
    }

    public boolean supportsInitializationScript() {
        return jdbc;
    }

    public boolean showsKeyClassField() {
        return jdbc;
    }

    public boolean showsPersistenceNameField() {
        return redis;
    }

    public boolean showsHostField() {
        return jdbc && network;
    }

    public boolean showsPortField() {
        return jdbc && network;
    }

    public boolean showsDatabaseField() {
        return jdbc;
    }

    public boolean showsSchemaField() {
        return jdbc && (schemaQualifier || this == DERBY);
    }

    public boolean showsPartTableField() {
        return jdbc;
    }

    public boolean showsCompletedLogTableField() {
        return jdbc;
    }

    public boolean showsUserField() {
        return jdbc && network;
    }

    public boolean showsPasswordField() {
        return jdbc && network;
    }

    public boolean showsRedisUriField() {
        return redis;
    }

    public boolean usesLocalDatabasePath() {
        return jdbc && !network;
    }

    public boolean usesDatabaseLookup() {
        return jdbc && network;
    }

    public boolean usesDirectoryDatabasePath() {
        return this == DERBY;
    }

    public boolean usesSchemaLookup() {
        return jdbc && schemaQualifier;
    }

    public String defaultDatabase() {
        return defaultDatabase;
    }

    public String defaultSchema() {
        return defaultSchema;
    }

    public Integer defaultPort() {
        return defaultPort;
    }

    public String defaultHost() {
        return network ? "localhost" : null;
    }

    public String defaultPartTable() {
        return jdbc ? "PART" : null;
    }

    public String defaultCompletedLogTable() {
        return jdbc ? "COMPLETED_LOG" : null;
    }

    public String defaultPersistenceName() {
        return redis ? "conveyor" : null;
    }

    public String defaultRedisUri() {
        return redis ? "redis://localhost:6379" : null;
    }

    public String databaseFieldLabel() {
        return switch (this) {
            case SQLITE -> "Database File";
            case DERBY -> "Database Home Directory";
            case REDIS -> "Database";
            default -> "Database";
        };
    }

    public String databaseActionLabel() {
        if (usesDatabaseLookup()) {
            return "Lookup";
        }
        if (usesLocalDatabasePath()) {
            return "Browse";
        }
        return "";
    }

    public String databaseActionToolTip() {
        if (usesDatabaseLookup()) {
            return "Lookup available databases";
        }
        if (usesLocalDatabasePath()) {
            return usesDirectoryDatabasePath()
                    ? "Select a local database directory"
                    : "Select a local database file";
        }
        return null;
    }

    public String databaseChooserTitle() {
        return switch (this) {
            case SQLITE -> "Select SQLite Database File";
            case DERBY -> "Select Derby Database Home Directory";
            default -> "Select Database";
        };
    }

    public String schemaFieldLabel() {
        return this == DERBY ? "Database Name" : "Schema";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
