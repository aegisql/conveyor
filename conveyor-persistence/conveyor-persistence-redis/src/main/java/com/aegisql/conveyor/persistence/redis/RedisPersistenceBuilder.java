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

    public RedisPersistenceBuilder(String name) {
        this(RedisConnectionFactory.resolveRedisUri(), null, name, true, 0, 100, 60_000L, new EncryptingConverterBuilder(), new HashSet<>(), cart -> true);
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
            Predicate<Cart<K, ?, ?>> persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                persistentPartFilter
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
                filter
        );
    }

    public Persistence<K> build() {
        LOG.debug(
                "Building RedisPersistence name={} uri={} autoInit={} minCompactSize={} maxArchiveBatchSize={} maxArchiveBatchTime={} encrypted={} nonPersistentProperties={} clientMode={}",
                name, redisUri, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder.get() != null, nonPersistentProperties,
                jedis == null ? "owned" : "external"
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

    Set<String> nonPersistentProperties() {
        return new HashSet<>(nonPersistentProperties);
    }

    Predicate<Cart<K, ?, ?>> persistentPartFilter() {
        return persistentPartFilter;
    }
}
