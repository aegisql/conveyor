package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.Load;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class RedisPersistence<K> implements Persistence<K> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisPersistence.class);
    static final String BACKEND_NAME = "redis";
    static final String BACKEND_VERSION = "1";

    private final RedisClientHandle clientHandle;
    private final JedisPooled jedis;
    private final String redisUri;
    private final String name;
    private final String namespace;
    private final int minCompactSize;
    private final int maxArchiveBatchSize;
    private final long maxArchiveBatchTime;
    private final Set<String> nonPersistentProperties;
    private final Predicate<Cart<K, ?, ?>> persistentPartFilter;
    private final SerializableToBytesConverter<Serializable> serializableConverter = new SerializableToBytesConverter<>();
    private final EncryptingConverter payloadEncryptor;
    private final ConverterAdviser<Object> plainConverterAdviser = new ConverterAdviser<>();
    private final ConverterAdviser<Object> payloadConverterAdviser;
    private final AtomicBoolean initialized;

    RedisPersistence(RedisPersistenceBuilder<K> builder) {
        this(builder, builder.jedis() == null
                ? RedisClientHandle.owned(RedisConnectionFactory.open(builder.connectionSettings()))
                : RedisClientHandle.external(builder.jedis()));
    }

    private RedisPersistence(RedisPersistenceBuilder<K> builder, RedisClientHandle clientHandle) {
        this.redisUri = builder.redisUri();
        this.name = builder.name();
        this.namespace = "conv:{" + name + "}";
        this.minCompactSize = builder.minCompactSize();
        this.maxArchiveBatchSize = builder.maxArchiveBatchSize();
        this.maxArchiveBatchTime = builder.maxArchiveBatchTime();
        this.nonPersistentProperties = builder.nonPersistentProperties();
        this.persistentPartFilter = builder.persistentPartFilter();
        this.payloadEncryptor = builder.encryptionBuilder().get();
        this.payloadConverterAdviser = newPayloadConverterAdviser(payloadEncryptor);
        this.clientHandle = clientHandle;
        this.jedis = clientHandle.jedis();
        this.initialized = new AtomicBoolean(false);
    }

    private RedisPersistence(RedisPersistence<K> source) {
        this.redisUri = source.redisUri;
        this.name = source.name;
        this.namespace = source.namespace;
        this.minCompactSize = source.minCompactSize;
        this.maxArchiveBatchSize = source.maxArchiveBatchSize;
        this.maxArchiveBatchTime = source.maxArchiveBatchTime;
        this.nonPersistentProperties = source.nonPersistentProperties;
        this.persistentPartFilter = source.persistentPartFilter;
        this.payloadEncryptor = source.payloadEncryptor;
        this.payloadConverterAdviser = newPayloadConverterAdviser(payloadEncryptor);
        this.clientHandle = source.clientHandle.retain();
        this.jedis = clientHandle.jedis();
        this.initialized = source.initialized;
    }

    private static ConverterAdviser<Object> newPayloadConverterAdviser(EncryptingConverter payloadEncryptor) {
        ConverterAdviser<Object> adviser = new ConverterAdviser<>();
        adviser.setEncryptor(payloadEncryptor);
        return adviser;
    }

    void init() {
        LOG.debug("Initializing Redis persistence namespace={} uri={}", namespace, redisUri);
        ensureInitialized();
    }

    @Override
    public long nextUniquePartId() {
        ensureInitialized();
        trackKey(seqKey());
        long id = jedis.incr(seqKey());
        LOG.debug("Generated next Redis part id {} for namespace={}", id, namespace);
        return id;
    }

    @Override
    public <L> void savePart(long id, Cart<K, ?, L> cart) {
        Objects.requireNonNull(cart, "cart must not be null");
        ensureInitialized();
        if (!isPartPersistent(cart)) {
            LOG.debug("Skipping non-persistent cart id={} cart={}", id, cart);
            return;
        }
        LOG.debug("Saving cart id={} loadType={} key={} label={} namespace={}",
                id, cart.getLoadType(), cart.getKey(), cart.getLabel(), namespace);
        LOG.trace("Saving itemized cart state id={} cart={}", id, cart);

        String payloadKey = payloadKey(id);
        String metaKey = metaKey(id);
        String reverseKeyIndex = reverseKeyIndexKey(id);
        String idMember = Long.toString(id);

        trackKey(payloadKey);
        trackKey(metaKey);
        trackKey(reverseKeyIndex);
        trackKey(activeIdsKey());
        trackKey(staticIdsKey());
        trackKey(expiringIdsKey());

        Map<String, Object> persistentProperties = extractPersistentProperties(cart);
        FieldEncoding keyEncoding = encodePlainField(cart.getKey());
        FieldEncoding labelEncoding = encodePlainField(cart.getLabel());
        FieldEncoding valueEncoding = encodePayloadField(cart.getLabel(), cart.getValue());
        FieldEncoding propertiesEncoding = encodePlainField(persistentProperties.isEmpty() ? null : (Serializable) persistentProperties);

        Map<String, String> meta = new HashMap<>();
        meta.put("id", idMember);
        meta.put("loadType", cart.getLoadType().name());
        meta.put("creationTime", Long.toString(cart.getCreationTime()));
        meta.put("expirationTime", Long.toString(cart.getExpirationTime()));
        meta.put("priority", Long.toString(cart.getPriority()));
        keyEncoding.put(meta, "keyHint", "keyData", false);
        labelEncoding.put(meta, "labelHint", "labelData", false);
        valueEncoding.put(meta, "valueHint", null, true);
        propertiesEncoding.put(meta, "propertiesHint", "propertiesData", false);

        if (cart instanceof GeneralCommand<?, ?> command && cart.getKey() == null) {
            FieldEncoding filterEncoding = encodePlainField(command.getFilter());
            filterEncoding.put(meta, "commandFilterHint", "commandFilterData", false);
        }

        jedis.del(metaKey);
        jedis.hset(metaKey, meta);

        if (valueEncoding.encoded() != null) {
            jedis.set(payloadKey, valueEncoding.encoded());
        } else {
            jedis.del(payloadKey);
        }

        if (cart.getLoadType() == LoadType.STATIC_PART) {
            jedis.zadd(staticIdsKey(), id, idMember);
            jedis.zrem(activeIdsKey(), idMember);
        } else {
            jedis.zadd(activeIdsKey(), id, idMember);
            jedis.zrem(staticIdsKey(), idMember);
        }

        long expirationTime = cart.getExpirationTime();
        if (expirationTime > 0) {
            jedis.zadd(expiringIdsKey(), expirationTime, idMember);
        } else {
            jedis.zrem(expiringIdsKey(), idMember);
        }

        if (cart.getKey() != null) {
            savePartId(cart.getKey(), id);
        }
    }

    @Override
    public <L> boolean isPartPersistent(Cart<K, ?, L> cart) {
        boolean persistent = persistentPartFilter.test(cart);
        LOG.trace("Cart persistence filter result={} cart={}", persistent, cart);
        return persistent;
    }

    @Override
    public void savePartId(K key, long partId) {
        if (key == null) {
            LOG.trace("Ignoring savePartId for null key and partId={}", partId);
            return;
        }
        ensureInitialized();
        String encodedKey = encodeSerializable(key);
        String partIdsKey = partIdsByKeyKey(encodedKey);
        String reverseIndexKey = reverseKeyIndexKey(partId);
        String idMember = Long.toString(partId);

        trackKey(partIdsKey);
        trackKey(reverseIndexKey);
        LOG.debug("Linking part id={} to cart key={} namespace={}", partId, key, namespace);
        jedis.zadd(partIdsKey, partId, idMember);
        jedis.sadd(reverseIndexKey, encodedKey);
    }

    @Override
    public void saveCompletedBuildKey(K key) {
        if (key == null) {
            LOG.trace("Ignoring saveCompletedBuildKey for null key");
            return;
        }
        ensureInitialized();
        trackKey(completedKeysKey());
        LOG.debug("Saving completed build key={} namespace={}", key, namespace);
        jedis.sadd(completedKeysKey(), encodeSerializable(key));
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getParts(Collection<Long> ids) {
        ensureInitialized();
        LOG.debug("Loading {} carts by ids from namespace={}", ids == null ? 0 : ids.size(), namespace);
        LOG.trace("Requested cart ids={}", ids);
        ArrayList<Cart<K, ?, L>> carts = new ArrayList<>();
        for (Long id : ids) {
            Cart<K, ?, L> cart = loadCart(id);
            if (cart != null) {
                carts.add(cart);
            }
        }
        return carts;
    }

    @Override
    public Collection<Long> getAllPartIds(K key) {
        if (key == null) {
            LOG.trace("Requested part ids for null key, returning empty list");
            return List.of();
        }
        ensureInitialized();
        Collection<Long> ids = jedis.zrange(partIdsByKeyKey(encodeSerializable(key)), 0, -1)
                .stream()
                .map(Long::parseLong)
                .toList();
        LOG.debug("Loaded {} part ids for key={} namespace={}", ids.size(), key, namespace);
        LOG.trace("Part ids for key={} => {}", key, ids);
        return ids;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getAllParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(jedis.zrange(activeIdsKey(), 0, -1));
        LOG.debug("Loaded {} active carts from namespace={}", carts.size(), namespace);
        LOG.trace("Active carts={}", carts);
        return carts;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getExpiredParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(jedis.zrangeByScore(expiringIdsKey(), Double.NEGATIVE_INFINITY, System.currentTimeMillis()));
        LOG.debug("Loaded {} expired carts from namespace={}", carts.size(), namespace);
        LOG.trace("Expired carts={}", carts);
        return carts;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(jedis.zrange(staticIdsKey(), 0, -1));
        LOG.debug("Loaded {} static carts from namespace={}", carts.size(), namespace);
        LOG.trace("Static carts={}", carts);
        return carts;
    }

    @Override
    public Set<K> getCompletedKeys() {
        ensureInitialized();
        Set<K> keys = jedis.smembers(completedKeysKey())
                .stream()
                .map(this::decodeSerializable)
                .map(this::castKey)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        LOG.debug("Loaded {} completed keys from namespace={}", keys.size(), namespace);
        LOG.trace("Completed keys={}", keys);
        return keys;
    }

    @Override
    public void archiveParts(Collection<Long> ids) {
        ensureInitialized();
        if (ids == null || ids.isEmpty()) {
            LOG.trace("archiveParts called with no ids for namespace={}", namespace);
            return;
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        LOG.debug("Archiving {} parts by id from namespace={}", uniqueIds.size(), namespace);
        LOG.trace("Archiving part ids={}", uniqueIds);
        for (Long id : uniqueIds) {
            String idMember = Long.toString(id);
            String reverseIndexKey = reverseKeyIndexKey(id);
            for (String encodedKey : jedis.smembers(reverseIndexKey)) {
                String partIdsKey = partIdsByKeyKey(encodedKey);
                jedis.zrem(partIdsKey, idMember);
                if (jedis.zcard(partIdsKey) == 0) {
                    jedis.del(partIdsKey);
                }
            }
            jedis.del(reverseIndexKey);
            jedis.zrem(activeIdsKey(), idMember);
            jedis.zrem(staticIdsKey(), idMember);
            jedis.zrem(expiringIdsKey(), idMember);
            jedis.del(payloadKey(id), metaKey(id));
        }
    }

    @Override
    public void archiveKeys(Collection<K> keys) {
        ensureInitialized();
        if (keys == null || keys.isEmpty()) {
            LOG.trace("archiveKeys called with no keys for namespace={}", namespace);
            return;
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        LOG.debug("Archiving {} cart keys from namespace={}", keys.size(), namespace);
        LOG.trace("Archiving cart keys={}", keys);
        for (K key : keys) {
            if (key == null) {
                continue;
            }
            String encodedKey = encodeSerializable(key);
            ids.addAll(getAllPartIds(key));
            jedis.del(partIdsByKeyKey(encodedKey));
        }
        archiveParts(ids);
    }

    @Override
    public void archiveCompleteKeys(Collection<K> keys) {
        ensureInitialized();
        if (keys == null || keys.isEmpty()) {
            LOG.trace("archiveCompleteKeys called with no keys for namespace={}", namespace);
            return;
        }
        String[] encodedKeys = keys.stream()
                .filter(Objects::nonNull)
                .map(this::encodeSerializable)
                .toArray(String[]::new);
        if (encodedKeys.length > 0) {
            LOG.debug("Archiving {} completed keys from namespace={}", encodedKeys.length, namespace);
            jedis.srem(completedKeysKey(), encodedKeys);
        }
    }

    @Override
    public void archiveExpiredParts() {
        ensureInitialized();
        List<Long> ids = jedis.zrangeByScore(expiringIdsKey(), Double.NEGATIVE_INFINITY, System.currentTimeMillis())
                .stream()
                .map(Long::parseLong)
                .toList();
        LOG.debug("Archiving expired parts count={} namespace={}", ids.size(), namespace);
        LOG.trace("Expired part ids to archive={}", ids);
        archiveParts(ids);
    }

    @Override
    public void archiveAll() {
        ensureInitialized();
        Set<String> trackedKeys = jedis.smembers(trackerKey());
        LOG.debug("Archiving all Redis persistence data for namespace={} trackedKeys={}", namespace, trackedKeys.size());
        LOG.trace("Tracked Redis keys scheduled for deletion={}", trackedKeys);
        if (!trackedKeys.isEmpty()) {
            jedis.del(trackedKeys.toArray(String[]::new));
        }
        jedis.del(trackerKey());
        initialized.set(false);
    }

    @Override
    public int getMaxArchiveBatchSize() {
        return maxArchiveBatchSize;
    }

    @Override
    public long getMaxArchiveBatchTime() {
        return maxArchiveBatchTime;
    }

    @Override
    public long getNumberOfParts() {
        ensureInitialized();
        long count = jedis.zcard(activeIdsKey()) + jedis.zcard(staticIdsKey());
        LOG.debug("Current number of Redis parts namespace={} count={}", namespace, count);
        return count;
    }

    @Override
    public int getMinCompactSize() {
        return minCompactSize;
    }

    @Override
    public Persistence<K> copy() {
        LOG.debug("Copying Redis persistence namespace={}", namespace);
        return new RedisPersistence<>(this);
    }

    @Override
    public boolean isPersistentProperty(String property) {
        boolean persistent = !nonPersistentProperties.contains(property);
        LOG.trace("Property '{}' persistent={} namespace={}", property, persistent, namespace);
        return persistent;
    }

    @Override
    public void close() throws IOException {
        LOG.debug("Closing Redis persistence namespace={} ownsClient={} refs={}",
                namespace, clientHandle.closesUnderlying(), clientHandle.referenceCount());
        clientHandle.close();
    }

    @Override
    public String toString() {
        return "RedisPersistence[" + redisUri + ", namespace=" + namespace + "]";
    }

    private void ensureInitialized() {
        if (initialized.get()) {
            return;
        }
        synchronized (initialized) {
            if (initialized.get()) {
                return;
            }
            Map<String, String> namespaceMeta = jedis.hgetAll(metaKey());
            if (namespaceMeta.isEmpty()) {
                bootstrapNamespaceMetadata();
            } else {
                validateNamespaceMetadata(namespaceMeta);
            }
            trackKey(metaKey());
            trackKey(seqKey());
            trackKey(activeIdsKey());
            trackKey(staticIdsKey());
            trackKey(expiringIdsKey());
            trackKey(completedKeysKey());
            initialized.set(true);
            LOG.trace("Ensured Redis namespace bootstrap for {}", namespace);
        }
    }

    private void bootstrapNamespaceMetadata() {
        LOG.debug("Bootstrapping Redis namespace metadata for {}", namespace);
        jedis.hset(metaKey(), Map.of(
                "backend", BACKEND_NAME,
                "version", BACKEND_VERSION,
                "name", name
        ));
    }

    private void validateNamespaceMetadata(Map<String, String> namespaceMeta) {
        String backend = namespaceMeta.get("backend");
        String version = namespaceMeta.get("version");
        String existingName = namespaceMeta.get("name");
        if (backend == null || version == null || existingName == null) {
            throw new PersistenceException("Redis namespace metadata is incomplete for " + namespace
                    + ". Expected backend, version, and name markers.");
        }
        if (!BACKEND_NAME.equals(backend)) {
            throw new PersistenceException("Redis namespace " + namespace + " belongs to backend '" + backend
                    + "', expected '" + BACKEND_NAME + "'");
        }
        if (!BACKEND_VERSION.equals(version)) {
            throw new PersistenceException("Redis namespace " + namespace + " uses version '" + version
                    + "', expected '" + BACKEND_VERSION + "'");
        }
        if (!name.equals(existingName)) {
            throw new PersistenceException("Redis namespace " + namespace + " is bound to name '" + existingName
                    + "', expected '" + name + "'");
        }
    }

    private Map<String, Object> extractPersistentProperties(Cart<K, ?, ?> cart) {
        Map<String, Object> properties = new HashMap<>();
        cart.getAllProperties().forEach((key, value) -> {
            if (isPersistentProperty(key)) {
                properties.put(key, value);
            }
        });
        return properties;
    }

    private <L> Collection<Cart<K, ?, L>> loadCarts(Collection<String> idMembers) {
        ArrayList<Cart<K, ?, L>> carts = new ArrayList<>();
        for (String idMember : idMembers) {
            Cart<K, ?, L> cart = loadCart(Long.parseLong(idMember));
            if (cart != null) {
                carts.add(cart);
            }
        }
        LOG.trace("Loaded {} carts for id members {}", carts.size(), idMembers);
        return carts;
    }

    @SuppressWarnings("unchecked")
    private <L> Cart<K, ?, L> loadCart(long id) {
        Map<String, String> meta = jedis.hgetAll(metaKey(id));
        String encodedPayload = jedis.get(payloadKey(id));
        if (meta.isEmpty() && encodedPayload == null) {
            LOG.trace("No cart data found for id={} namespace={}", id, namespace);
            return null;
        }
        if (isLegacyWholeCartFormat(meta, encodedPayload)) {
            LOG.trace("Decoding legacy whole-cart payload for id={} namespace={}", id, namespace);
            return (Cart<K, ?, L>) decodeLegacyWholeCart(encodedPayload);
        }

        try {
            LoadType loadType = LoadType.valueOf(meta.get("loadType"));
            long creationTime = parseLong(meta.get("creationTime"));
            long expirationTime = parseLong(meta.get("expirationTime"));
            long priority = parseLong(meta.get("priority"));
            K key = castKey(decodePlainField(meta.get("keyHint"), meta.get("keyData")));
            Object label = decodePlainField(meta.get("labelHint"), meta.get("labelData"));
            Object value = decodePayloadField(label, meta.get("valueHint"), encodedPayload, meta.get("valueData"));
            Map<String, Object> properties = decodeProperties(meta.get("propertiesHint"), meta.get("propertiesData"));

            if (loadType == LoadType.COMMAND) {
                CommandLabel commandLabel = (CommandLabel) label;
                GeneralCommand<K, Object> command;
                if (key != null) {
                    command = new GeneralCommand<>(key, value, commandLabel, creationTime, expirationTime);
                } else {
                    Predicate<K> filter = castFilter(decodePlainField(meta.get("commandFilterHint"), meta.get("commandFilterData")));
                    command = new GeneralCommand<>(filter, value, commandLabel, creationTime, expirationTime);
                }
                command.putAllProperties(properties);
                return castCart(command);
            }

            if (loadType == LoadType.BUILDER) {
                CreatingCart<K, Object, L> cart = new CreatingCart<>(key, asBuilderSupplier(value), creationTime, expirationTime, properties, priority);
                return castCart(cart);
            }

            if (loadType == LoadType.RESULT_CONSUMER) {
                ResultConsumerCart<K, Object, L> cart = new ResultConsumerCart<>(key, asResultConsumer(value), creationTime, expirationTime, priority);
                cart.putAllProperties(properties);
                return castCart(cart);
            }

            if (loadType == LoadType.MULTI_KEY_PART) {
                Load<K, Object> load = asLoad(value);
                return castCart(new MultiKeyCart<>(load.getFilter(), load.getValue(), castLabel(label), creationTime,
                        expirationTime, load.getLoadType(), properties, priority));
            }

            return castCart(new ShoppingCart<>(key, value, castLabel(label), creationTime, expirationTime, properties, loadType, priority));
        } catch (Exception e) {
            LOG.error("Failed to decode Redis cart id={} namespace={} meta={}", id, namespace, meta, e);
            throw new PersistenceException("Failed to read Redis part cart", e);
        }
    }

    private boolean isLegacyWholeCartFormat(Map<String, String> meta, String encodedPayload) {
        return encodedPayload != null
                && !meta.isEmpty()
                && !meta.containsKey("valueHint")
                && !meta.containsKey("keyHint")
                && !meta.containsKey("labelHint");
    }

    private long parseLong(String value) {
        return value == null ? 0L : Long.parseLong(value);
    }

    private FieldEncoding encodePayloadField(Object label, Object value) {
        return encodeField(payloadConverterAdviser, label, value, true);
    }

    private FieldEncoding encodePlainField(Object value) {
        return encodeField(plainConverterAdviser, null, value, false);
    }

    private FieldEncoding encodeField(ConverterAdviser<Object> adviser, Object label, Object value, boolean includeNullHint) {
        String typeHint = value == null ? null : value.getClass().getTypeName();
        ObjectConverter<Object, byte[]> converter = adviser.getConverter(label, typeHint);
        byte[] bytes = converter.toPersistence(value);
        if (value == null && !includeNullHint) {
            return FieldEncoding.empty();
        }
        return new FieldEncoding(converter.conversionHint(), encodeBytes(bytes));
    }

    private Serializable decodeLegacyWholeCart(String encodedValue) {
        byte[] bytes = Base64.getUrlDecoder().decode(encodedValue);
        if (payloadEncryptor != null) {
            LOG.trace("Decrypting legacy whole-cart Redis payload namespace={}", namespace);
            bytes = payloadEncryptor.fromPersistence(bytes);
        }
        return serializableConverter.fromPersistence(bytes);
    }

    private Object decodePayloadField(Object label, String hint, String encodedPayload, String legacyEncodedValue) {
        String encodedValue = encodedPayload != null ? encodedPayload : legacyEncodedValue;
        if (encodedValue == null) {
            return null;
        }
        if (encodedPayload == null && legacyEncodedValue != null) {
            LOG.trace("Decoding payload from legacy mirrored valueData field namespace={}", namespace);
        }
        ObjectConverter<Object, byte[]> converter = payloadConverterAdviser.getConverter(label, hint);
        return converter.fromPersistence(decodeBytes(encodedValue));
    }

    private Object decodePlainField(String hint, String encodedValue) {
        if (hint == null && encodedValue == null) {
            return null;
        }
        ObjectConverter<Object, byte[]> converter = plainConverterAdviser.getConverter(null, hint);
        return converter.fromPersistence(decodeBytes(encodedValue));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeProperties(String hint, String encodedValue) {
        Map<String, Object> properties = (Map<String, Object>) decodePlainField(hint, encodedValue);
        return properties == null ? new HashMap<>() : properties;
    }

    private String encodeSerializable(Object value) {
        if (value == null) {
            throw new PersistenceException("Redis persistence cannot encode null values as indexed keys");
        }
        if (!(value instanceof Serializable serializable)) {
            throw new PersistenceException("Value is not serializable: " + value.getClass().getName());
        }
        return encodeBytes(serializableConverter.toPersistence(serializable));
    }

    private Serializable decodeSerializable(String encodedValue) {
        return serializableConverter.fromPersistence(decodeBytes(encodedValue));
    }

    private String encodeBytes(byte[] bytes) {
        return bytes == null ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decodeBytes(String encodedValue) {
        return encodedValue == null ? null : Base64.getUrlDecoder().decode(encodedValue);
    }

    @SuppressWarnings("unchecked")
    private K castKey(Object key) {
        return (K) key;
    }

    @SuppressWarnings("unchecked")
    private <L> L castLabel(Object label) {
        return (L) label;
    }

    @SuppressWarnings("unchecked")
    private Predicate<K> castFilter(Object filter) {
        return (Predicate<K>) filter;
    }

    @SuppressWarnings("unchecked")
    private BuilderSupplier<Object> asBuilderSupplier(Object value) {
        return (BuilderSupplier<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private ResultConsumer<K, Object> asResultConsumer(Object value) {
        return (ResultConsumer<K, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private Load<K, Object> asLoad(Object value) {
        return (Load<K, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private <L> Cart<K, ?, L> castCart(Cart<K, ?, ?> cart) {
        return (Cart<K, ?, L>) cart;
    }

    private void trackKey(String key) {
        if (!trackerKey().equals(key)) {
            jedis.sadd(trackerKey(), key);
        }
    }

    private String trackerKey() {
        return namespace + ":tracker";
    }

    private String metaKey() {
        return namespace + ":meta";
    }

    private String seqKey() {
        return namespace + ":seq";
    }

    private String activeIdsKey() {
        return namespace + ":parts:active";
    }

    private String staticIdsKey() {
        return namespace + ":parts:static";
    }

    private String expiringIdsKey() {
        return namespace + ":parts:expires";
    }

    private String completedKeysKey() {
        return namespace + ":completed";
    }

    private String payloadKey(long id) {
        return namespace + ":part:" + id + ":payload";
    }

    private String metaKey(long id) {
        return namespace + ":part:" + id + ":meta";
    }

    private String reverseKeyIndexKey(long id) {
        return namespace + ":part:" + id + ":keys";
    }

    private String partIdsByKeyKey(String encodedKey) {
        return namespace + ":parts:key:" + encodedKey;
    }

    private record FieldEncoding(String hint, String encoded) {
        static FieldEncoding empty() {
            return new FieldEncoding(null, null);
        }

        void put(Map<String, String> meta, String hintField, String dataField, boolean includeNullHint) {
            if (hint != null && (includeNullHint || encoded != null)) {
                meta.put(hintField, hint);
            }
            if (dataField != null && encoded != null) {
                meta.put(dataField, encoded);
            }
        }
    }
}
