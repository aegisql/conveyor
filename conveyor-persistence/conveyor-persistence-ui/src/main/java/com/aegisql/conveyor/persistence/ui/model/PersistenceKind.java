package com.aegisql.conveyor.persistence.ui.model;

public enum PersistenceKind {
    MYSQL("MySQL", "mysql", true, false, true, true, null, null, 3306),
    MARIADB("MariaDB", "mariadb", true, false, true, true, null, null, 3306),
    POSTGRES("PostgreSQL", "postgres", true, false, true, true, null, null, 5432),
    ORACLE("Oracle", "oracle", true, false, true, true, null, null, 1521),
    SQLSERVER("SQL Server", "sqlserver", true, false, true, true, null, null, 1433),
    SQLITE("SQLite", "sqlite", true, false, false, false, "conveyor.db", null, null),
    DERBY("Derby Embedded", "derby", true, false, false, false, "conveyor-db", "conveyor-db", null),
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
            case DERBY -> "Database Path";
            case REDIS -> "Database";
            default -> "Database";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
