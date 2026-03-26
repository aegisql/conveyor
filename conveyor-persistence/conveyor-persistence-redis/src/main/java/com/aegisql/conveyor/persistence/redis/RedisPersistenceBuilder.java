package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Field;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;

import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.net.URI;
import java.io.IOException;
import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
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
    private final PriorityRestoreStrategy priorityRestoreStrategy;
    private final List<Field<?>> additionalFields;
    private final List<ConverterRegistration> converterRegistrations;
    private final ArchiveOptions<K> archiveOptions;
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
                PriorityRestoreStrategy.JAVA_SORT,
                List.of(),
                List.of(),
                ArchiveOptions.delete(),
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
            PriorityRestoreStrategy priorityRestoreStrategy,
            List<Field<?>> additionalFields,
            List<ConverterRegistration> converterRegistrations,
            ArchiveOptions<K> archiveOptions,
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
        this.priorityRestoreStrategy = Objects.requireNonNull(priorityRestoreStrategy, "priorityRestoreStrategy must not be null");
        this.additionalFields = validatedAdditionalFields(additionalFields);
        this.converterRegistrations = List.copyOf(Objects.requireNonNull(converterRegistrations, "converterRegistrations must not be null"));
        this.archiveOptions = Objects.requireNonNull(archiveOptions, "archiveOptions must not be null");
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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

    public RedisPersistenceBuilder<K> doNotSaveCartProperties(String property, String... more) {
        Objects.requireNonNull(property, "property must not be null");
        HashSet<String> updated = new HashSet<>(nonPersistentProperties);
        updated.add(property);
        if (more != null) {
            updated.addAll(Arrays.asList(more));
        }
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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
        Objects.requireNonNull(filter, "filter must not be null");
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
                priorityRestoreStrategy,
                additionalFields,
                converterRegistrations,
                archiveOptions,
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

    public RedisPersistenceBuilder<K> addCartPersistenceFilter(Predicate<Cart<K, ?, ?>> partFilter) {
        Objects.requireNonNull(partFilter, "partFilter must not be null");
        return persistentPartFilter(partFilter.and(persistentPartFilter));
    }

    public <L> RedisPersistenceBuilder<K> addLabelPersistenceFilter(Predicate<L> labelFilter) {
        Objects.requireNonNull(labelFilter, "labelFilter must not be null");
        return addCartPersistenceFilter(cart -> labelFilter.test(cast(cart.getLabel())));
    }

    public <L, V> RedisPersistenceBuilder<K> addLabelValuePersistenceFilter(BiPredicate<L, V> labelValueFilter) {
        Objects.requireNonNull(labelValueFilter, "labelValueFilter must not be null");
        return addCartPersistenceFilter(cart -> labelValueFilter.test(cast(cart.getLabel()), cast(cart.getValue())));
    }

    public RedisPersistenceBuilder<K> fields(List<? extends Field<?>> fields) {
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
                persistentPartFilter,
                restoreOrder,
                priorityRestoreStrategy,
                validatedAdditionalFields(fields),
                converterRegistrations,
                archiveOptions,
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

    public <T> RedisPersistenceBuilder<K> addField(Class<T> fieldClass, String name) {
        ArrayList<Field<?>> updated = new ArrayList<>(additionalFields);
        updated.add(new Field<>(fieldClass, name));
        return fields(updated);
    }

    public <T> RedisPersistenceBuilder<K> addField(Class<T> fieldClass, String name, Function<Cart<?, ?, ?>, T> accessor) {
        ArrayList<Field<?>> updated = new ArrayList<>(additionalFields);
        updated.add(new Field<>(fieldClass, name, accessor));
        return fields(updated);
    }

    public <T> RedisPersistenceBuilder<K> addBinaryConverter(Class<T> valueClass, ObjectConverter<T, byte[]> converter) {
        Objects.requireNonNull(valueClass, "valueClass must not be null");
        Objects.requireNonNull(converter, "converter must not be null");
        ArrayList<ConverterRegistration> updated = new ArrayList<>(converterRegistrations);
        updated.add(ConverterRegistration.forClass(valueClass, converter));
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, updated, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public <L, T> RedisPersistenceBuilder<K> addBinaryConverter(L label, ObjectConverter<T, byte[]> converter) {
        Objects.requireNonNull(converter, "converter must not be null");
        ArrayList<ConverterRegistration> updated = new ArrayList<>(converterRegistrations);
        updated.add(ConverterRegistration.forLabel(label, converter));
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, updated, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> maxTotal(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("maxTotal must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, value, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> maxIdle(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("maxIdle must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, value, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> minIdle(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("minIdle must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, value, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> connectionTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("connectionTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, value,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> socketTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("socketTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                value, blockingSocketTimeoutMillis, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> blockingSocketTimeoutMillis(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("blockingSocketTimeoutMillis must be greater than 0");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, value, database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> database(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("database must not be negative");
        }
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, value, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> clientName(String value) {
        Objects.requireNonNull(value, "clientName must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, value, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> user(String value) {
        Objects.requireNonNull(value, "user must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, value, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> password(String value) {
        Objects.requireNonNull(value, "password must not be null");
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, value, ssl
        );
    }

    public RedisPersistenceBuilder<K> ssl(boolean value) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions, maxTotal, maxIdle, minIdle, connectionTimeoutMillis,
                socketTimeoutMillis, blockingSocketTimeoutMillis, database, clientName, user, password, value
        );
    }

    public RedisPersistenceBuilder<K> restoreOrder(RestoreOrder value) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, Objects.requireNonNull(value, "restoreOrder must not be null"), priorityRestoreStrategy, additionalFields, converterRegistrations, archiveOptions,
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> priorityRestoreStrategy(PriorityRestoreStrategy value) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, Objects.requireNonNull(value, "priorityRestoreStrategy must not be null"), additionalFields, converterRegistrations, archiveOptions,
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> deleteArchiving() {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, ArchiveOptions.delete(),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> noArchiving() {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, ArchiveOptions.noAction(),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> archiver(Persistence<K> archivingPersistence) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, ArchiveOptions.moveToPersistence(Objects.requireNonNull(archivingPersistence, "archivingPersistence must not be null")),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> archiver(BinaryLogConfiguration binaryLogConfiguration) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, ArchiveOptions.moveToFile(Objects.requireNonNull(binaryLogConfiguration, "binaryLogConfiguration must not be null")),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public RedisPersistenceBuilder<K> archiver(Archiver<K> customArchiver) {
        return new RedisPersistenceBuilder<>(
                redisUri, jedis, name, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, encryptionBuilder,
                nonPersistentProperties, persistentPartFilter, restoreOrder, priorityRestoreStrategy, additionalFields, converterRegistrations, ArchiveOptions.custom(Objects.requireNonNull(customArchiver, "customArchiver must not be null")),
                maxTotal, maxIdle, minIdle, connectionTimeoutMillis, socketTimeoutMillis, blockingSocketTimeoutMillis,
                database, clientName, user, password, ssl
        );
    }

    public Persistence<K> build() {
        RedisConnectionSettings connectionSettings = connectionSettings();
        LOG.debug(
                "Building RedisPersistence name={} uri={} autoInit={} minCompactSize={} maxArchiveBatchSize={} maxArchiveBatchTime={} restoreOrder={} priorityRestoreStrategy={} archiveStrategy={} encrypted={} nonPersistentProperties={} converterRegistrations={} clientMode={} poolTuned={} clientTuned={}",
                name, redisUri, autoInit, minCompactSize, maxArchiveBatchSize, maxArchiveBatchTime, restoreOrder, priorityRestoreStrategy, archiveOptions.archiveStrategy(), encryptionBuilder.get() != null, nonPersistentProperties,
                converterRegistrations.size(), jedis == null ? "owned" : "external", connectionSettings.hasCustomPoolConfig(), connectionSettings.hasCustomClientConfig()
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
        registerMBean(persistence);
        return persistence;
    }

    public String getJMXObjName() {
        return "com.aegisql.conveyor.persistence.redis." + name + ":type=" + name;
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

    PriorityRestoreStrategy priorityRestoreStrategy() {
        return priorityRestoreStrategy;
    }

    List<Field<?>> additionalFields() {
        return new ArrayList<>(additionalFields);
    }

    List<ConverterRegistration> converterRegistrations() {
        return new ArrayList<>(converterRegistrations);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    ArchiveOptions<K> archiveOptions() {
        return archiveOptions;
    }

    ArchiveStrategy archiveStrategy() {
        return archiveOptions.archiveStrategy();
    }

    private void registerMBean(Persistence<K> persistence) {
        try {
            String objName = getJMXObjName();
            LOG.debug("Redis JMX name {}", objName);
            ObjectName objectName = new ObjectName(objName);
            synchronized (RedisPersistenceMBean.mBeanServer) {
                if (!RedisPersistenceMBean.mBeanServer.isRegistered(objectName)) {
                    RedisPersistenceMBean<K> redisMBean = new RedisPersistenceMBean<>() {
                        @Override
                        public String getBackend() {
                            return RedisPersistence.BACKEND_NAME;
                        }

                        @Override
                        public String getRedisUri() {
                            return sanitizedRedisUri();
                        }

                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getNamespace() {
                            return "conv:{" + name + "}";
                        }

                        @Override
                        public String getArchiveStrategy() {
                            return archiveOptions.archiveStrategy().name();
                        }

                        @Override
                        public boolean isEncrypted() {
                            return encryptionBuilder.get() != null;
                        }

                        @Override
                        public String getRestoreOrder() {
                            return restoreOrder.name();
                        }

                        @Override
                        public String getPriorityRestoreStrategy() {
                            return priorityRestoreStrategy.name();
                        }

                        @Override
                        public int getMaxBatchSize() {
                            return maxArchiveBatchSize;
                        }

                        @Override
                        public long getMaxBatchTime() {
                            return maxArchiveBatchTime;
                        }

                        @Override
                        public int minCompactSize() {
                            return minCompactSize;
                        }

                        @Override
                        public boolean isAutoInit() {
                            return autoInit;
                        }

                        @Override
                        public boolean isExternalClient() {
                            return jedis != null;
                        }

                        @Override
                        public int getAdditionalFieldCount() {
                            return additionalFields.size();
                        }

                        @Override
                        public int getConverterCount() {
                            return converterRegistrations.size();
                        }

                        @Override
                        public Persistence<K> get() {
                            return persistence;
                        }
                    };
                    StandardMBean mbean = new StandardMBean(redisMBean, RedisPersistenceMBean.class);
                    RedisPersistenceMBean.mBeanServer.registerMBean(mbean, objectName);
                }
            }
        } catch (Exception e) {
            throw new PersistenceException("Failed to register Redis persistence MBean", e);
        }
    }

    private String sanitizedRedisUri() {
        URI uri = URI.create(redisUri);
        int port = uri.getPort() > 0 ? uri.getPort() : 6379;
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String fragment = uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment();
        return uri.getScheme() + "://" + uri.getHost() + ":" + port + path + query + fragment;
    }

    private static List<Field<?>> validatedAdditionalFields(List<? extends Field<?>> fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        ArrayList<Field<?>> validated = new ArrayList<>();
        HashSet<String> names = new HashSet<>();
        for (Field<?> field : fields) {
            Objects.requireNonNull(field, "field must not be null");
            validateFieldName(field.getName());
            if (!names.add(field.getName())) {
                throw new IllegalArgumentException("Duplicate additional field name: " + field.getName());
            }
            validated.add(field);
        }
        return validated;
    }

    private static void validateFieldName(String name) {
        Objects.requireNonNull(name, "field name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Additional field name must not be blank");
        }
        if (!trimmed.equals(name)) {
            throw new IllegalArgumentException("Additional field name must not have surrounding whitespace");
        }
        if (trimmed.contains(":")) {
            throw new IllegalArgumentException("Additional field name must not contain ':'");
        }
        if (Set.of(
                "id",
                "loadType",
                "creationTime",
                "expirationTime",
                "priority",
                "priorityIndexMember",
                "keyHint",
                "keyData",
                "labelHint",
                "labelData",
                "valueHint",
                "valueData",
                "propertiesHint",
                "propertiesData",
                "commandFilterHint",
                "commandFilterData"
        ).contains(trimmed)) {
            throw new IllegalArgumentException("Additional field name collides with reserved Redis metadata name: " + trimmed);
        }
    }

    static final class ArchiveOptions<K> {
        private final ArchiveStrategy archiveStrategy;
        private final Archiver<K> customArchiver;
        private final Persistence<K> archivingPersistence;
        private final BinaryLogConfiguration binaryLogConfiguration;

        private ArchiveOptions(
                ArchiveStrategy archiveStrategy,
                Archiver<K> customArchiver,
                Persistence<K> archivingPersistence,
                BinaryLogConfiguration binaryLogConfiguration
        ) {
            this.archiveStrategy = Objects.requireNonNull(archiveStrategy, "archiveStrategy must not be null");
            this.customArchiver = customArchiver;
            this.archivingPersistence = archivingPersistence;
            this.binaryLogConfiguration = binaryLogConfiguration;
        }

        static <K> ArchiveOptions<K> delete() {
            return new ArchiveOptions<>(ArchiveStrategy.DELETE, null, null, null);
        }

        static <K> ArchiveOptions<K> noAction() {
            return new ArchiveOptions<>(ArchiveStrategy.NO_ACTION, null, null, null);
        }

        static <K> ArchiveOptions<K> moveToPersistence(Persistence<K> archivingPersistence) {
            return new ArchiveOptions<>(ArchiveStrategy.MOVE_TO_PERSISTENCE, null, archivingPersistence, null);
        }

        static <K> ArchiveOptions<K> moveToFile(BinaryLogConfiguration binaryLogConfiguration) {
            return new ArchiveOptions<>(ArchiveStrategy.MOVE_TO_FILE, null, null, binaryLogConfiguration);
        }

        static <K> ArchiveOptions<K> custom(Archiver<K> customArchiver) {
            return new ArchiveOptions<>(ArchiveStrategy.CUSTOM, customArchiver, null, null);
        }

        ArchiveStrategy archiveStrategy() {
            return archiveStrategy;
        }

        Archiver<K> customArchiver() {
            return customArchiver;
        }

        Persistence<K> archivingPersistence() {
            return archivingPersistence;
        }

        BinaryLogConfiguration binaryLogConfiguration() {
            return binaryLogConfiguration;
        }
    }

    static final class ConverterRegistration {
        private final Class<?> valueClass;
        private final Object label;
        private final ObjectConverter<?, byte[]> converter;

        private ConverterRegistration(Class<?> valueClass, Object label, ObjectConverter<?, byte[]> converter) {
            this.valueClass = valueClass;
            this.label = label;
            this.converter = converter;
        }

        static ConverterRegistration forClass(Class<?> valueClass, ObjectConverter<?, byte[]> converter) {
            return new ConverterRegistration(valueClass, null, converter);
        }

        static ConverterRegistration forLabel(Object label, ObjectConverter<?, byte[]> converter) {
            return new ConverterRegistration(null, label, converter);
        }

        @SuppressWarnings("unchecked")
        void apply(ConverterAdviser<Object> adviser) {
            if (valueClass != null) {
                adviser.addConverter(valueClass, (ObjectConverter<Object, byte[]>) converter);
            } else {
                adviser.addConverter(label, (ObjectConverter<Object, byte[]>) converter);
            }
        }
    }
}
