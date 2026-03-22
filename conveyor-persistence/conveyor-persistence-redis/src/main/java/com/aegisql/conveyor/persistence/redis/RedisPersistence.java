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
import com.aegisql.conveyor.persistence.archive.ArchiveStrategy;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.archive.DoNothingArchiver;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.redis.archive.DeleteRedisArchiver;
import com.aegisql.conveyor.persistence.redis.archive.FileRedisArchiver;
import com.aegisql.conveyor.persistence.redis.archive.PersistenceRedisArchiver;
import com.aegisql.conveyor.persistence.redis.archive.RedisArchiveAccess;
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
import java.util.Comparator;
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
    private final RestoreOrder restoreOrder;
    private final int minCompactSize;
    private final int maxArchiveBatchSize;
    private final long maxArchiveBatchTime;
    private final Set<String> nonPersistentProperties;
    private final Predicate<Cart<K, ?, ?>> persistentPartFilter;
    private final RedisPersistenceBuilder.ArchiveOptions<K> archiveOptions;
    private final SerializableToBytesConverter<Serializable> serializableConverter = new SerializableToBytesConverter<>();
    private final EncryptingConverter payloadEncryptor;
    private final ConverterAdviser<Object> plainConverterAdviser = new ConverterAdviser<>();
    private final ConverterAdviser<Object> payloadConverterAdviser;
    private final RedisLuaScriptBundle luaScripts;
    private final AtomicBoolean initialized;
    private final Archiver<K> archiver;

    RedisPersistence(RedisPersistenceBuilder<K> builder) {
        this(builder, builder.jedis() == null
                ? RedisClientHandle.owned(RedisConnectionFactory.open(builder.connectionSettings()))
                : RedisClientHandle.external(builder.jedis()));
    }

    private RedisPersistence(RedisPersistenceBuilder<K> builder, RedisClientHandle clientHandle) {
        this.redisUri = builder.redisUri();
        this.name = builder.name();
        this.namespace = "conv:{" + name + "}";
        this.restoreOrder = builder.restoreOrder();
        this.minCompactSize = builder.minCompactSize();
        this.maxArchiveBatchSize = builder.maxArchiveBatchSize();
        this.maxArchiveBatchTime = builder.maxArchiveBatchTime();
        this.nonPersistentProperties = builder.nonPersistentProperties();
        this.persistentPartFilter = builder.persistentPartFilter();
        this.archiveOptions = builder.archiveOptions();
        this.payloadEncryptor = builder.encryptionBuilder().get();
        this.payloadConverterAdviser = newPayloadConverterAdviser(payloadEncryptor);
        this.clientHandle = clientHandle;
        this.jedis = clientHandle.jedis();
        this.luaScripts = new RedisLuaScriptBundle(jedis);
        this.initialized = new AtomicBoolean(false);
        this.archiver = buildArchiver();
        this.archiver.setPersistence(this);
    }

    private RedisPersistence(RedisPersistence<K> source) {
        this.redisUri = source.redisUri;
        this.name = source.name;
        this.namespace = source.namespace;
        this.restoreOrder = source.restoreOrder;
        this.minCompactSize = source.minCompactSize;
        this.maxArchiveBatchSize = source.maxArchiveBatchSize;
        this.maxArchiveBatchTime = source.maxArchiveBatchTime;
        this.nonPersistentProperties = source.nonPersistentProperties;
        this.persistentPartFilter = source.persistentPartFilter;
        this.archiveOptions = source.archiveOptions;
        this.payloadEncryptor = source.payloadEncryptor;
        this.payloadConverterAdviser = newPayloadConverterAdviser(payloadEncryptor);
        this.clientHandle = source.clientHandle.retain();
        this.jedis = clientHandle.jedis();
        this.luaScripts = new RedisLuaScriptBundle(jedis);
        this.initialized = source.initialized;
        this.archiver = buildArchiver();
        this.archiver.setPersistence(this);
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

        String encodedIndexedKey = cart.getKey() == null ? null : encodeSerializable(cart.getKey());
        luaScripts.savePart(
                savePartKeys(payloadKey, metaKey, reverseKeyIndex, encodedIndexedKey),
                savePartArgs(idMember, cart, valueEncoding.encoded(), encodedIndexedKey, meta)
        );
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
        Collection<String> ids = jedis.zrange(partIdsByKeyKey(encodeSerializable(key)), 0, -1)
                .stream()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collection<Long> orderedIds = orderedIdMembers(ids)
                .stream()
                .map(Long::parseLong)
                .toList();
        LOG.debug("Loaded {} part ids for key={} namespace={} restoreOrder={}", orderedIds.size(), key, namespace, restoreOrder);
        LOG.trace("Part ids for key={} => {}", key, orderedIds);
        return orderedIds;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getAllParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(orderedIdMembers(jedis.zrange(activeIdsKey(), 0, -1)));
        LOG.debug("Loaded {} active carts from namespace={} restoreOrder={}", carts.size(), namespace, restoreOrder);
        LOG.trace("Active carts={}", carts);
        return carts;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getExpiredParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(orderedIdMembers(jedis.zrangeByScore(expiringIdsKey(), Double.NEGATIVE_INFINITY, System.currentTimeMillis())));
        LOG.debug("Loaded {} expired carts from namespace={} restoreOrder={}", carts.size(), namespace, restoreOrder);
        LOG.trace("Expired carts={}", carts);
        return carts;
    }

    @Override
    public <L> Collection<Cart<K, ?, L>> getAllStaticParts() {
        ensureInitialized();
        Collection<Cart<K, ?, L>> carts = loadCarts(orderedIdMembers(jedis.zrange(staticIdsKey(), 0, -1)));
        LOG.debug("Loaded {} static carts from namespace={} restoreOrder={}", carts.size(), namespace, restoreOrder);
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
        archiver.archiveParts(ids);
    }

    @Override
    public void archiveKeys(Collection<K> keys) {
        ensureInitialized();
        archiver.archiveKeys(keys);
    }

    @Override
    public void archiveCompleteKeys(Collection<K> keys) {
        ensureInitialized();
        archiver.archiveCompleteKeys(keys);
    }

    @Override
    public void archiveExpiredParts() {
        ensureInitialized();
        archiver.archiveExpiredParts();
    }

    @Override
    public void archiveAll() {
        ensureInitialized();
        archiver.archiveAll();
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
            RedisBootstrapValidator.RedisServerVersion serverVersion = RedisBootstrapValidator.validate(jedis, namespace);
            Map<String, String> namespaceMeta = jedis.hgetAll(metaKey());
            if (namespaceMeta.isEmpty()) {
                bootstrapNamespaceMetadata();
            } else {
                validateNamespaceMetadata(namespaceMeta);
            }
            ensureScriptBundleMetadata(jedis.hgetAll(metaKey()));
            luaScripts.ensureLoaded();
            trackKey(metaKey());
            trackKey(seqKey());
            trackKey(activeIdsKey());
            trackKey(staticIdsKey());
            trackKey(expiringIdsKey());
            trackKey(completedKeysKey());
            initialized.set(true);
            LOG.trace("Ensured Redis namespace bootstrap for {} on server {}", namespace, serverVersion);
        }
    }

    private Archiver<K> buildArchiver() {
        Archiver<K> builtArchiver = switch (archiveOptions.archiveStrategy()) {
            case CUSTOM -> archiveOptions.customArchiver();
            case DELETE -> new DeleteRedisArchiver<>(newArchiveAccess());
            case MOVE_TO_PERSISTENCE -> new PersistenceRedisArchiver<>(newArchiveAccess(), archiveOptions.archivingPersistence());
            case MOVE_TO_FILE -> new FileRedisArchiver<>(newArchiveAccess(), archiveOptions.binaryLogConfiguration());
            case NO_ACTION -> new DoNothingArchiver<>();
            case SET_ARCHIVED -> throw new PersistenceException("SET_ARCHIVED is intentionally unsupported for Redis persistence");
        };
        if (builtArchiver == null) {
            throw new PersistenceException("Redis archive strategy " + archiveOptions.archiveStrategy() + " is not configured correctly");
        }
        return builtArchiver;
    }

    private RedisArchiveAccess<K> newArchiveAccess() {
        return new RedisArchiveAccess<>() {
            @Override
            public void deleteParts(Collection<Long> ids) {
                deletePartsInternal(ids);
            }

            @Override
            public void deleteCompletedKeys(Collection<K> keys) {
                deleteCompletedKeysInternal(keys);
            }

            @Override
            public void deleteAll() {
                deleteAllInternal();
            }

            @Override
            public Collection<Long> expiredPartIds() {
                return expiredPartIdsInternal();
            }
        };
    }

    private void bootstrapNamespaceMetadata() {
        LOG.debug("Bootstrapping Redis namespace metadata for {}", namespace);
        jedis.hset(metaKey(), Map.of(
                "backend", BACKEND_NAME,
                "version", BACKEND_VERSION,
                "name", name,
                "scriptMode", RedisLuaScriptBundle.SCRIPT_MODE,
                "scriptBundleVersion", RedisLuaScriptBundle.BUNDLE_VERSION
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

    private void ensureScriptBundleMetadata(Map<String, String> namespaceMeta) {
        String scriptMode = namespaceMeta.get("scriptMode");
        String scriptBundleVersion = namespaceMeta.get("scriptBundleVersion");
        if (scriptMode == null && scriptBundleVersion == null) {
            jedis.hset(metaKey(), Map.of(
                    "scriptMode", RedisLuaScriptBundle.SCRIPT_MODE,
                    "scriptBundleVersion", RedisLuaScriptBundle.BUNDLE_VERSION
            ));
            return;
        }
        if (scriptMode == null || scriptBundleVersion == null) {
            throw new PersistenceException("Redis namespace metadata is incomplete for " + namespace
                    + ". Expected scriptMode and scriptBundleVersion markers once Lua bootstrap metadata is present.");
        }
        if (!RedisLuaScriptBundle.SCRIPT_MODE.equals(scriptMode)) {
            throw new PersistenceException("Redis namespace " + namespace + " uses script mode '" + scriptMode
                    + "', expected '" + RedisLuaScriptBundle.SCRIPT_MODE + "'");
        }
        if (!RedisLuaScriptBundle.BUNDLE_VERSION.equals(scriptBundleVersion)) {
            throw new PersistenceException("Redis namespace " + namespace + " uses script bundle version '" + scriptBundleVersion
                    + "', expected '" + RedisLuaScriptBundle.BUNDLE_VERSION + "'");
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

    private List<String> savePartKeys(String payloadKey, String metaKey, String reverseKeyIndex, String encodedKey) {
        ArrayList<String> keys = new ArrayList<>();
        keys.add(trackerKey());
        keys.add(metaKey);
        keys.add(payloadKey);
        keys.add(reverseKeyIndex);
        keys.add(activeIdsKey());
        keys.add(staticIdsKey());
        keys.add(expiringIdsKey());
        if (encodedKey != null) {
            keys.add(partIdsByKeyKey(encodedKey));
        }
        return keys;
    }

    private List<String> savePartArgs(String idMember, Cart<K, ?, ?> cart, String encodedPayload, String encodedKey, Map<String, String> meta) {
        ArrayList<String> args = new ArrayList<>();
        args.add(idMember);
        args.add(Long.toString(cart.getExpirationTime()));
        args.add(cart.getLoadType() == LoadType.STATIC_PART ? "1" : "0");
        args.add(encodedPayload == null ? "0" : "1");
        args.add(encodedPayload == null ? "" : encodedPayload);
        args.add(encodedKey == null ? "0" : "1");
        args.add(encodedKey == null ? "" : encodedKey);
        args.add(Integer.toString(meta.size()));
        meta.forEach((field, value) -> {
            args.add(field);
            args.add(value);
        });
        return args;
    }

    private void deletePartsInternal(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOG.trace("archiveParts called with no ids for namespace={}", namespace);
            return;
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        LOG.debug("Deleting {} archived parts by id from namespace={} strategy={}", uniqueIds.size(), namespace, archiveOptions.archiveStrategy());
        LOG.trace("Deleting archived part ids={}", uniqueIds);
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

    private void deleteCompletedKeysInternal(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            LOG.trace("archiveCompleteKeys called with no keys for namespace={}", namespace);
            return;
        }
        String[] encodedKeys = keys.stream()
                .filter(Objects::nonNull)
                .map(this::encodeSerializable)
                .toArray(String[]::new);
        if (encodedKeys.length > 0) {
            LOG.debug("Deleting {} completed keys from namespace={} strategy={}", encodedKeys.length, namespace, archiveOptions.archiveStrategy());
            jedis.srem(completedKeysKey(), encodedKeys);
        }
    }

    private Collection<Long> expiredPartIdsInternal() {
        List<Long> ids = jedis.zrangeByScore(expiringIdsKey(), Double.NEGATIVE_INFINITY, System.currentTimeMillis())
                .stream()
                .map(Long::parseLong)
                .toList();
        LOG.debug("Collected {} expired part ids from namespace={} strategy={}", ids.size(), namespace, archiveOptions.archiveStrategy());
        LOG.trace("Expired part ids={}", ids);
        return ids;
    }

    private void deleteAllInternal() {
        Set<String> trackedKeys = jedis.smembers(trackerKey());
        LOG.debug("Deleting all Redis persistence data for namespace={} trackedKeys={} strategy={}", namespace, trackedKeys.size(), archiveOptions.archiveStrategy());
        LOG.trace("Tracked Redis keys scheduled for deletion={}", trackedKeys);
        if (!trackedKeys.isEmpty()) {
            jedis.del(trackedKeys.toArray(String[]::new));
        }
        jedis.del(trackerKey());
        initialized.set(false);
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

    private List<String> orderedIdMembers(Collection<String> idMembers) {
        ArrayList<String> ordered = new ArrayList<>(idMembers);
        switch (restoreOrder) {
            case NO_ORDER -> {
                return ordered;
            }
            case BY_ID -> ordered.sort(Comparator.comparingLong(Long::parseLong));
            case BY_PRIORITY_AND_ID -> ordered.sort(Comparator
                    .comparingLong(this::priorityOfIdMember).reversed()
                    .thenComparingLong(Long::parseLong));
        }
        return ordered;
    }

    private long priorityOfIdMember(String idMember) {
        return parseLong(jedis.hget(metaKey(Long.parseLong(idMember)), "priority"));
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
