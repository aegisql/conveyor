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
        String redisUri,
        PayloadEncryptionMode payloadEncryptionMode,
        String encryptionSecret
) {

    private static final String LEGACY_DERBY_DATABASE_NAME = "conveyor-db";
    private static final String DEFAULT_DERBY_DATABASE_NAME = "conveyor_db";

    public PersistenceProfile {
        Objects.requireNonNull(kind, "kind must not be null");
    }

    public PersistenceProfile(
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
        this(
                id,
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
                redisUri,
                PayloadEncryptionMode.NONE,
                null
        );
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
                kind.defaultRedisUri(),
                PayloadEncryptionMode.NONE,
                null
        );
        return profile.normalized();
    }

    public PersistenceProfile normalized() {
        String normalizedDatabase = normalizedDatabase();
        String normalizedSchema = normalizedSchema();
        String normalizedPartTable = kind.isJdbc() ? defaultIfBlank(partTable, kind.defaultPartTable()) : trimToNull(partTable);
        String normalizedPersistenceName = kind.isRedis() ? defaultIfBlank(persistenceName, kind.defaultPersistenceName()) : trimToNull(persistenceName);
        PayloadEncryptionMode normalizedEncryptionMode = payloadEncryptionMode == null ? PayloadEncryptionMode.NONE : payloadEncryptionMode;
        return new PersistenceProfile(
                id,
                defaultIfBlank(displayName, suggestedDisplayName(kind, normalizedDatabase, normalizedPartTable, normalizedPersistenceName)),
                kind,
                kind.isJdbc() ? defaultIfBlank(keyClassName, "java.lang.Long") : trimToNull(keyClassName),
                normalizedPersistenceName,
                kind.isNetwork() ? defaultIfBlank(host, kind.defaultHost()) : trimToNull(host),
                port != null ? port : kind.defaultPort(),
                normalizedDatabase,
                normalizedSchema,
                normalizedPartTable,
                kind.isJdbc() ? defaultIfBlank(completedLogTable, kind.defaultCompletedLogTable()) : trimToNull(completedLogTable),
                trimToNull(user),
                trimToNull(password),
                kind.isRedis() ? defaultIfBlank(redisUri, kind.defaultRedisUri()) : trimToNull(redisUri),
                normalizedEncryptionMode,
                trimToNull(encryptionSecret)
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
                redisUri,
                payloadEncryptionMode,
                encryptionSecret
        );
    }

    public PersistenceProfile withPassword(String newPassword) {
        return new PersistenceProfile(
                id,
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
                newPassword,
                redisUri,
                payloadEncryptionMode,
                encryptionSecret
        );
    }

    public PersistenceProfile withEncryptionSecret(String newEncryptionSecret) {
        return new PersistenceProfile(
                id,
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
                redisUri,
                payloadEncryptionMode,
                newEncryptionSecret
        );
    }

    public PersistenceProfile withPayloadEncryptionMode(PayloadEncryptionMode newPayloadEncryptionMode) {
        return new PersistenceProfile(
                id,
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
                redisUri,
                newPayloadEncryptionMode,
                encryptionSecret
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

    private String normalizedSchema() {
        String normalized = trimToNull(schema);
        if (kind != PersistenceKind.DERBY) {
            return normalized;
        }
        if (normalized == null || LEGACY_DERBY_DATABASE_NAME.equals(normalized)) {
            return DEFAULT_DERBY_DATABASE_NAME;
        }
        return normalized;
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
