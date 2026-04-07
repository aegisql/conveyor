package com.aegisql.conveyor.persistence.ui.model;

import java.util.Objects;

public record PersistenceProfile(
        Long id,
        String displayName,
        PersistenceKind kind,
        String keyClassName,
        String persistenceName,
        String host,
        Integer port,
        String database,
        String schema,
        String partTable,
        String completedLogTable,
        String user,
        String password,
        String redisUri
) {

    public PersistenceProfile {
        Objects.requireNonNull(kind, "kind must not be null");
    }

    public static PersistenceProfile defaults(PersistenceKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        PersistenceProfile profile = new PersistenceProfile(
                null,
                null,
                kind,
                kind.isJdbc() ? "java.lang.Long" : null,
                kind.defaultPersistenceName(),
                kind.defaultHost(),
                kind.defaultPort(),
                kind.defaultDatabase(),
                kind.defaultSchema(),
                kind.defaultPartTable(),
                kind.defaultCompletedLogTable(),
                null,
                null,
                kind.defaultRedisUri()
        );
        return profile.normalized();
    }

    public PersistenceProfile normalized() {
        String normalizedDatabase = normalizedDatabase();
        String normalizedPartTable = kind.isJdbc() ? defaultIfBlank(partTable, kind.defaultPartTable()) : trimToNull(partTable);
        String normalizedPersistenceName = kind.isRedis() ? defaultIfBlank(persistenceName, kind.defaultPersistenceName()) : trimToNull(persistenceName);
        return new PersistenceProfile(
                id,
                defaultIfBlank(displayName, suggestedDisplayName(kind, normalizedDatabase, normalizedPartTable, normalizedPersistenceName)),
                kind,
                kind.isJdbc() ? defaultIfBlank(keyClassName, "java.lang.Long") : trimToNull(keyClassName),
                normalizedPersistenceName,
                kind.isNetwork() ? defaultIfBlank(host, kind.defaultHost()) : trimToNull(host),
                port != null ? port : kind.defaultPort(),
                normalizedDatabase,
                trimToNull(schema),
                normalizedPartTable,
                kind.isJdbc() ? defaultIfBlank(completedLogTable, kind.defaultCompletedLogTable()) : trimToNull(completedLogTable),
                trimToNull(user),
                trimToNull(password),
                kind.isRedis() ? defaultIfBlank(redisUri, kind.defaultRedisUri()) : trimToNull(redisUri)
        );
    }

    public PersistenceProfile withId(Long newId) {
        return new PersistenceProfile(
                newId,
                displayName,
                kind,
                keyClassName,
                persistenceName,
                host,
                port,
                database,
                schema,
                partTable,
                completedLogTable,
                user,
                password,
                redisUri
        );
    }

    public PersistenceProfile duplicateAsNew() {
        return withId(null);
    }

    public String label() {
        return normalized().displayName();
    }

    public String suggestedDisplayName() {
        PersistenceProfile normalized = normalized();
        return suggestedDisplayName(normalized.kind(), normalized.database(), normalized.partTable(), normalized.persistenceName());
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : trimToNull(defaultValue);
    }

    private String normalizedDatabase() {
        if (kind.isJdbc() && kind.isNetwork()) {
            return trimToNull(database);
        }
        return defaultIfBlank(database, kind.defaultDatabase());
    }

    public static String suggestedDisplayName(PersistenceKind kind, String database, String partTable, String persistenceName) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind.isJdbc()) {
            String normalizedDatabase = trimToNull(database);
            String normalizedPartTable = trimToNull(partTable);
            if (normalizedDatabase != null && normalizedPartTable != null) {
                return normalizedDatabase + " / " + normalizedPartTable;
            }
            if (normalizedDatabase != null) {
                return normalizedDatabase;
            }
        }
        if (kind.isRedis()) {
            String normalizedPersistenceName = trimToNull(persistenceName);
            if (normalizedPersistenceName != null) {
                return normalizedPersistenceName;
            }
        }
        return kind.displayName() + " Connection";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
