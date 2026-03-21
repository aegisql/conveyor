package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class RedisPersistenceBuilder<K> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistenceBuilder.class);

    private final String redisUri;
    private final JedisPooled jedis;
    private final String name;
    private final boolean autoInit;
    private final int minCompactSize;
    private final int maxArchiveBatchSize;
    private final long maxArchiveBatchTime;
    private final EncryptingConverterBuilder encryptionBuilder;
    private final Set<String> nonPersistentProperties;
    private final Predicate<Cart<K, ?, ?>> persistentPartFilter;
    private final RestoreOrder restoreOrder;
    private final Integer maxTotal;
    private final Integer maxIdle;
    private final Integer minIdle;
    private final Integer connectionTimeoutMillis;
    private final Integer socketTimeoutMillis;
    private final Integer blockingSocketTimeoutMillis;
    private final Integer database;
    private final String clientName;
    private final String user;
    private final String password;
    private final Boolean ssl;

    public RedisPersistenceBuilder(String name) {
        this(
                RedisConnectionFactory.resolveRedisUri(),
                null,
                name,
                true,
                0,
                100,
                60_000L,
                new EncryptingConverterBuilder(),
                new HashSet<>(),
                cart -> true,
                RestoreOrder.BY_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private RedisPersistenceBuilder(
            String redisUri,
            JedisPooled jedis,
            String name,
            boolean autoInit,
            int minCompactSize,
            int maxArchiveBatchSize,
            long maxArchiveBatchTime,
            EncryptingConverterBuilder encryptionBuilder,
            Set<String> nonPersistentProperties,
            Predicate<Cart<K, ?, ?>> persistentPartFilter,
            RestoreOrder restoreOrder,
            Integer maxTotal,
            Integer maxIdle,
            Integer minIdle,
            Integer connectionTimeoutMillis,
            Integer socketTimeoutMillis,
            Integer blockingSocketTimeoutMillis,
            Integer database,
            String clientName,
            String user,
            String password,
            Boolean ssl
    ) {
        this.redisUri = Objects.requireNonNull(redisUri, "redisUri must not be null");
        this.jedis = jedis;
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.autoInit = autoInit;
        this.minCompactSize = minCompactSize;
        this.maxArchiveBatchSize = maxArchiveBatchSize;
        this.maxArchiveBatchTime = maxArchiveBatchTime;
        this.encryptionBuilder = Objects.requireNonNull(encryptionBuilder, "encryptionBuilder must not be null");
        this.nonPersistentProperties = new HashSet<>(nonPersistentProperties);
        this.persistentPartFilter = Objects.requireNonNull(persistentPartFilter, "persistentPartFilter must not be null");
        this.restoreOrder = Objects.requireNonNull(restoreOrder, "restoreOrder must not be null");
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.socketTimeoutMillis = socketTimeoutMillis;
        this.blockingSocketTimeoutMillis = blockingSocketTimeoutMillis;
        this.database = database;
        this.clientName = clientName;
        this.user = user;
        this.password = password;
        this.ssl = ssl;
    }

    public RedisPersistenceBuilder<K> redisUri(String value) {
        return new RedisPersistenceBuilder<>(
                value,
                null,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> jedis(JedisPooled value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                Objects.requireNonNull(value, "jedis must not be null"),
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> autoInit(boolean value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                value,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> minCompactSize(int value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                value,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> maxArchiveBatchSize(int value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                value,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> maxArchiveBatchTime(long value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                value,
                encryptionBuilder,
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> encryptionSecret(String value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder.encryptionSecret(value),
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> encryptionSecret(SecretKey value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder.secretKey(value),
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> encryptionAlgorithm(String value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder.encryptionAlgorithm(value),
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> encryptionTransformation(String value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder.encryptionTransformation(value),
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> encryptionKeyLength(int value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder.encryptionKeyLength(value),
                nonPersistentProperties,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> nonPersistentProperty(String property) {
        Objects.requireNonNull(property, "property must not be null");
        HashSet<String> updated = new HashSet<>(nonPersistentProperties);
        updated.add(property);
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                updated,
                persistentPartFilter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> persistentPartFilter(Predicate<Cart<K, ?, ?>> filter) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                jedis,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                encryptionBuilder,
                nonPersistentProperties,
                filter,
                restoreOrder,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    public RedisPersistenceBuilder<K> maxTotal(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("maxTotal must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, value, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> maxIdle(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("maxIdle must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, value, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> minIdle(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("minIdle must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, value, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> connectionTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("connectionTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, value,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> socketTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("socketTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                value, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> blockingSocketTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("blockingSocketTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, value, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> database(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("database must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, value, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> clientName(String value) {
        Objects.requireNonNull(value, "clientName must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, value, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> user(String value) {
        Objects.requireNonNull(value, "user must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, value, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> password(String value) {
        Objects.requireNonNull(value, "password must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, value, ssl
        );
    }

    public RedisPersistenceBuilder<K> ssl(boolean value) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, value
        );
    }

    public RedisPersistenceBuilder<K> restoreOrder(RestoreOrder value) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, Objects.requireNonNull(value, "restoreOrder must not be null"),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public Persistence<K> build() {
        RedisConnectionSettings connectionSettings = connectionSettings();
        LOG.debug(
                "Building RedisPersistence name={} uri={} autoInit={} minCompactSize={} maxArchiveBatchSize={} maxArchiveBatchTime={} restoreOrder={} encrypted={} nonPersistentProperties={} clientMode={} poolTuned={} clientTuned={}",
                name, redisUri, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, restoreOrder, encryptionBuilder.get() != null, nonPersistentProperties,
                jedis == null ? "owned" : "external", connectionSettings.hasCustomPoolConfig(), connectionSettings.hasCustomClientConfig()
        );
        RedisPersistence<K> persistence = new RedisPersistence<>(this);
        if (autoInit) {
            try {
                persistence.init();
            } catch (RuntimeException e) {
                try {
                    persistence.close();
                } catch (IOException closeError) {
                    e.addSuppressed(closeError);
                }
                throw e;
            }
        }
        return persistence;
    }

    String redisUri() {
        return redisUri;
    }

    JedisPooled jedis() {
        return jedis;
    }

    String name() {
        return name;
    }

    int minCompactSize() {
        return minCompactSize;
    }

    int maxArchiveBatchSize() {
        return maxArchiveBatchSize;
    }

    long maxArchiveBatchTime() {
        return maxArchiveBatchTime;
    }

    EncryptingConverterBuilder encryptionBuilder() {
        return encryptionBuilder;
    }

    RedisConnectionSettings connectionSettings() {
        return new RedisConnectionSettings(
                redisUri,
                maxTotal,
                maxIdle,
                minIdle,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                blockingSocketTimeoutMillis,
                database,
                clientName,
                user,
                password,
                ssl
        );
    }

    Set<String> nonPersistentProperties() {
        return new HashSet<>(nonPersistentProperties);
    }

    Predicate<Cart<K, ?, ?>> persistentPartFilter() {
        return persistentPartFilter;
    }

    RestoreOrder restoreOrder() {
        return restoreOrder;
    }
}
