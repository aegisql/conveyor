package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class RedisPersistenceBuilder<K> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistenceBuilder.class);

    private final String redisUri;
    private final String name;
    private final boolean autoInit;
    private final int minCompactSize;
    private final int maxArchiveBatchSize;
    private final long maxArchiveBatchTime;
    private final Set<String> nonPersistentProperties;
    private final Predicate<Cart<K, ?, ?>> persistentPartFilter;

    public RedisPersistenceBuilder(String name) {
        this(RedisConnectionFactory.resolveRedisUri(), name, true, 0, 100, 60_000L, new HashSet<>(), cart -> true);
    }

    private RedisPersistenceBuilder(
            String redisUri,
            String name,
            boolean autoInit,
            int minCompactSize,
            int maxArchiveBatchSize,
            long maxArchiveBatchTime,
            Set<String> nonPersistentProperties,
            Predicate<Cart<K, ?, ?>> persistentPartFilter
    ) {
        this.redisUri = Objects.requireNonNull(redisUri, "redisUri must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.autoInit = autoInit;
        this.minCompactSize = minCompactSize;
        this.maxArchiveBatchSize = maxArchiveBatchSize;
        this.maxArchiveBatchTime = maxArchiveBatchTime;
        this.nonPersistentProperties = new HashSet<>(nonPersistentProperties);
        this.persistentPartFilter = Objects.requireNonNull(persistentPartFilter, "persistentPartFilter must not be null");
    }

    public RedisPersistenceBuilder<K> redisUri(String value) {
        return new RedisPersistenceBuilder<>(
                value,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                nonPersistentProperties,
                persistentPartFilter
        );
    }

    public RedisPersistenceBuilder<K> autoInit(boolean value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                name,
                value,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                nonPersistentProperties,
                persistentPartFilter
        );
    }

    public RedisPersistenceBuilder<K> minCompactSize(int value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                name,
                autoInit,
                value,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                nonPersistentProperties,
                persistentPartFilter
        );
    }

    public RedisPersistenceBuilder<K> maxArchiveBatchSize(int value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                name,
                autoInit,
                minCompactSize,
                value,
                maxArchiveBatchTime,
                nonPersistentProperties,
                persistentPartFilter
        );
    }

    public RedisPersistenceBuilder<K> maxArchiveBatchTime(long value) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                value,
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
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                updated,
                persistentPartFilter
        );
    }

    public RedisPersistenceBuilder<K> persistentPartFilter(Predicate<Cart<K, ?, ?>> filter) {
        return new RedisPersistenceBuilder<>(
                redisUri,
                name,
                autoInit,
                minCompactSize,
                maxArchiveBatchSize,
                maxArchiveBatchTime,
                nonPersistentProperties,
                filter
        );
    }

    public Persistence<K> build() {
        LOG.debug(
                "Building RedisPersistence name={} uri={} autoInit={} minCompactSize={} maxArchiveBatchSize={} maxArchiveBatchTime={} nonPersistentProperties={}",
                name, redisUri, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, nonPersistentProperties
        );
        RedisPersistence<K> persistence = new RedisPersistence<>(this);
        if (autoInit) {
            persistence.init();
        }
        return persistence;
    }

    String redisUri() {
        return redisUri;
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

    Set<String> nonPersistentProperties() {
        return new HashSet<>(nonPersistentProperties);
    }

    Predicate<Cart<K, ?, ?>> persistentPartFilter() {
        return persistentPartFilter;
    }
}
