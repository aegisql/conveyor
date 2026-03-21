package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.serial.SerializablePredicate;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPersistenceTest extends RedisTestSupport {

    private final SerializableToBytesConverter<Serializable> serializableConverter = new SerializableToBytesConverter<>();

    @Test
    void supportsBasicPersistenceContractMethods() throws Exception {
        openRedis().close();

        RedisPersistenceBuilder<Integer> builder = new RedisPersistenceBuilder<Integer>(testNamespace("basic"))
                .minCompactSize(17)
                .maxArchiveBatchSize(23)
                .maxArchiveBatchTime(4_567L)
                .nonPersistentProperty("TEMP");

        try (Persistence<Integer> persistence = builder.build()) {
            persistence.archiveAll();

            assertEquals(1L, persistence.nextUniquePartId());
            long id = persistence.nextUniquePartId();
            assertEquals(2L, id);
            assertEquals(17, persistence.getMinCompactSize());
            assertEquals(23, persistence.getMaxArchiveBatchSize());
            assertEquals(4_567L, persistence.getMaxArchiveBatchTime());
            assertTrue(persistence.isPersistentProperty("KEEP"));
            assertFalse(persistence.isPersistentProperty("TEMP"));

            ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(101, "alpha", "A");
            cart.addProperty("KEEP", "yes");
            cart.addProperty("TEMP", "no");

            assertTrue(persistence.isPartPersistent(cart));
            persistence.savePart(id, cart);

            Cart<Integer, ?, String> restored = persistence.getPart(id);
            assertNotNull(restored);
            assertEquals(101, restored.getKey());
            assertEquals("alpha", restored.getValue());
            assertEquals("A", restored.getLabel());

            Collection<Cart<Integer, ?, String>> selected = persistence.getParts(List.of(id));
            assertEquals(1, selected.size());

            Collection<Long> allPartIds = persistence.getAllPartIds(101);
            assertEquals(List.of(id), new ArrayList<>(allPartIds));

            Collection<Cart<Integer, ?, String>> allParts = persistence.getAllParts();
            assertEquals(1, allParts.size());
            assertEquals(1L, persistence.getNumberOfParts());

            try (Persistence<Integer> copy = persistence.copy()) {
                assertNotNull(copy.getPart(id));
                assertEquals(1L, copy.getNumberOfParts());
                assertEquals(23, copy.getMaxArchiveBatchSize());
                assertEquals(4_567L, copy.getMaxArchiveBatchTime());
            }
        }
    }

    @Test
    void supportsManualKeyIndexCompletedKeysAndArchiveOperations() throws Exception {
        openRedis().close();

        try (Persistence<Integer> persistence = newPersistence("archive")) {
            persistence.archiveAll();

            long shoppingId = persistence.nextUniquePartId();
            persistence.savePart(shoppingId, new ShoppingCart<>(1, "one", "L1"));

            long multiKeyId = persistence.nextUniquePartId();
            MultiKeyCart<Integer, String, String> multiKeyCart =
                    new MultiKeyCart<>((SerializablePredicate<Integer>) key -> key > 0, "shared", "L2", 0, 0);
            persistence.savePart(multiKeyId, multiKeyCart);
            persistence.savePartId(2, multiKeyId);

            persistence.saveCompletedBuildKey(1);
            persistence.saveCompletedBuildKey(2);
            assertEquals(Set.of(1, 2), persistence.getCompletedKeys());

            assertEquals(List.of(shoppingId), new ArrayList<>(persistence.getAllPartIds(1)));
            assertEquals(List.of(multiKeyId), new ArrayList<>(persistence.getAllPartIds(2)));

            persistence.archiveCompleteKeys(Set.of(1));
            assertEquals(Set.of(2), persistence.getCompletedKeys());
            assertNotNull(persistence.getPart(shoppingId));

            persistence.archiveKeys(List.of(2));
            assertTrue(persistence.getAllPartIds(2).isEmpty());
            assertNull(persistence.getPart(multiKeyId));

            persistence.archiveParts(List.of(shoppingId));
            assertNull(persistence.getPart(shoppingId));
            assertEquals(0L, persistence.getNumberOfParts());
        }
    }

    @Test
    void supportsStaticAndExpiredQueriesAndArchiveAll() throws Exception {
        openRedis().close();

        try (Persistence<Integer> persistence = newPersistence("expiry")) {
            persistence.archiveAll();

            long now = System.currentTimeMillis();
            Cart<Integer, String, String> staticCart =
                    new ShoppingCart<>(null, "static-value", "STATIC", now, now + 60_000, null, LoadType.STATIC_PART, 0);
            Cart<Integer, String, String> expiringCart =
                    new ShoppingCart<>(1, "soon", "PART", now, now + 250);
            Cart<Integer, String, String> normalCart =
                    new ShoppingCart<>(2, "later", "PART", now, now + 60_000);

            long staticId = persistence.nextUniquePartId();
            long expiringId = persistence.nextUniquePartId();
            long normalId = persistence.nextUniquePartId();

            persistence.savePart(staticId, staticCart);
            persistence.savePart(expiringId, expiringCart);
            persistence.savePart(normalId, normalCart);

            assertEquals(2, persistence.getAllParts().size());
            assertEquals(1, persistence.getAllStaticParts().size());
            assertEquals(3L, persistence.getNumberOfParts());

            Thread.sleep(500);

            Collection<Cart<Integer, ?, String>> expiredParts = persistence.getExpiredParts();
            assertEquals(1, expiredParts.size());
            assertEquals("soon", expiredParts.iterator().next().getValue());

            persistence.archiveExpiredParts();
            assertNull(persistence.getPart(expiringId));
            assertNotNull(persistence.getPart(staticId));
            assertNotNull(persistence.getPart(normalId));

            persistence.saveCompletedBuildKey(2);
            assertEquals(Set.of(2), persistence.getCompletedKeys());

            persistence.archiveAll();
            assertTrue(persistence.getAllParts().isEmpty());
            assertTrue(persistence.getAllStaticParts().isEmpty());
            assertTrue(persistence.getCompletedKeys().isEmpty());
            assertEquals(0L, persistence.getNumberOfParts());
            assertEquals(1L, persistence.nextUniquePartId());
        }
    }

    @Test
    void roundTripsSerializableSpecializedCartTypes() throws Exception {
        openRedis().close();

        try (Persistence<Integer> persistence = newPersistence("specialized")) {
            persistence.archiveAll();

            BuilderSupplier<StringBuilder> builderSupplier = () -> StringBuilder::new;
            ResultConsumer<Integer, String> resultConsumer = bin -> { };

            long creatingId = persistence.nextUniquePartId();
            long consumerId = persistence.nextUniquePartId();
            long multiKeyId = persistence.nextUniquePartId();

            persistence.savePart(creatingId, new CreatingCart<>(11, builderSupplier, 1, 2, 3));
            persistence.savePart(consumerId, new ResultConsumerCart<>(12, resultConsumer, 3, 4, 5));
            persistence.savePart(multiKeyId, new MultiKeyCart<>((SerializablePredicate<Integer>) key -> key >= 0, "value", "MK", 5, 6, 7));

            Cart<Integer, ?, ?> creatingCart = persistence.getPart(creatingId);
            Cart<Integer, ?, ?> consumerCart = persistence.getPart(consumerId);
            Cart<Integer, ?, ?> multiKeyCart = persistence.getPart(multiKeyId);

            assertInstanceOf(CreatingCart.class, creatingCart);
            assertEquals(LoadType.BUILDER, creatingCart.getLoadType());
            assertNotNull(((CreatingCart<Integer, StringBuilder, String>) creatingCart).getValue().get().get());

            assertInstanceOf(ResultConsumerCart.class, consumerCart);
            assertEquals(LoadType.RESULT_CONSUMER, consumerCart.getLoadType());
            assertNotNull(consumerCart.getValue());

            assertInstanceOf(MultiKeyCart.class, multiKeyCart);
            assertEquals(LoadType.MULTI_KEY_PART, multiKeyCart.getLoadType());
            assertEquals("value", ((MultiKeyCart<Integer, String, String>) multiKeyCart).getValue().getValue());
        }
    }

    @Test
    void replaysPersistedCommandCartsWhenWrappingPersistentConveyor() throws Exception {
        openRedis().close();

        String name = testNamespace("command-replay");
        long commandId;

        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name).build()) {
            writer.archiveAll();
            commandId = writer.nextUniquePartId();

            GeneralCommand<Integer, Boolean> suspendCommand =
                    new GeneralCommand<>((SerializablePredicate<Integer>) key -> true, true, CommandLabel.SUSPEND, System.currentTimeMillis(), 0);
            writer.savePart(commandId, suspendCommand);

            Cart<Integer, ?, ?> persistedCommand = writer.getPart(commandId);
            assertNotNull(persistedCommand);
            assertEquals(LoadType.COMMAND, persistedCommand.getLoadType());
            assertInstanceOf(GeneralCommand.class, persistedCommand);
        }

        try (Persistence<Integer> reader = new RedisPersistenceBuilder<Integer>(name).build()) {
            AssemblingConveyor<Integer, String, String> forward = new AssemblingConveyor<>();
            forward.setName("redis-command-replay");
            PersistentConveyor<Integer, String, String> persistent = reader.wrapConveyor(forward);
            try {
                awaitTrue(forward::isSuspended, "Persisted SUSPEND command should suspend the wrapped conveyor when recovered");
            } finally {
                persistent.stop();
            }
        }
    }

    @Test
    void supportsAbsorbDefaultMethod() throws Exception {
        openRedis().close();

        try (Persistence<Integer> source = newPersistence("absorb-source");
             Persistence<Integer> target = newPersistence("absorb-target")) {
            source.archiveAll();
            target.archiveAll();

            long keepId = source.nextUniquePartId();
            source.savePart(keepId, new ShoppingCart<>(10, "keep", "A"));

            long completedId = source.nextUniquePartId();
            source.savePart(completedId, new ShoppingCart<>(20, "skip", "B"));
            source.saveCompletedBuildKey(20);

            target.absorb(source);

            Collection<Cart<Integer, ?, Object>> absorbed = target.getAllParts();
            assertEquals(1, absorbed.size());

            Cart<Integer, ?, Object> restored = absorbed.iterator().next();
            assertEquals(10, restored.getKey());
            assertEquals("keep", restored.getValue());
            assertEquals(1, restored.getProperty("RECOVERY_ATTEMPT", Integer.class));
            assertNotNull(restored.getProperty("#CART_ID", Long.class));
        }
    }

    @Test
    void supportsPersistentConveyorFactoryHelpers() throws Exception {
        openRedis().close();

        try (Persistence<Integer> wrappedPersistence = newPersistence("helpers-wrap");
             Persistence<Integer> defaultPersistence = newPersistence("helpers-default");
             Persistence<Integer> suppliedPersistence = newPersistence("helpers-supplied")) {
            PersistentConveyor<Integer, String, String> wrapped =
                    wrappedPersistence.wrapConveyor(new AssemblingConveyor<>());
            PersistentConveyor<Integer, String, String> defaultConveyor = defaultPersistence.getConveyor();
            PersistentConveyor<Integer, String, String> suppliedConveyor =
                    suppliedPersistence.getConveyor(AssemblingConveyor::new);

            assertNotNull(wrapped);
            assertNotNull(defaultConveyor);
            assertNotNull(suppliedConveyor);
        }
    }

    @Test
    void returnsPartsInCurrentRedisIdOrderAssumptions() throws Exception {
        openRedis().close();

        try (Persistence<Integer> persistence = newPersistence("restore-order")) {
            persistence.archiveAll();

            long now = System.currentTimeMillis();
            persistence.savePart(30L, new ShoppingCart<>(7, "third", "A30", now, 0, null, LoadType.PART, 500));
            persistence.savePart(10L, new ShoppingCart<>(7, "first", "A10", now, 0, null, LoadType.PART, -100));
            persistence.savePart(20L, new ShoppingCart<>(7, "second", "A20", now, 0, null, LoadType.PART, 999));

            persistence.savePart(40L, new ShoppingCart<>(null, "static-fourth", "S40", now, 0, null, LoadType.STATIC_PART, -10));
            persistence.savePart(5L, new ShoppingCart<>(null, "static-first", "S05", now, 0, null, LoadType.STATIC_PART, 700));
            persistence.savePart(25L, new ShoppingCart<>(null, "static-second", "S25", now, 0, null, LoadType.STATIC_PART, 0));

            assertEquals(List.of(10L, 20L, 30L), new ArrayList<>(persistence.getAllPartIds(7)));
            assertEquals(
                    List.of("first", "second", "third"),
                    persistence.getAllParts().stream().map(Cart::getValue).toList()
            );
            assertEquals(
                    List.of("static-first", "static-second", "static-fourth"),
                    persistence.getAllStaticParts().stream().map(Cart::getValue).toList()
            );
        }
    }

    @Test
    void storesItemizedRedisCartStructureInsteadOfWholeCartPayloads() throws Exception {
        openRedis().close();

        String name = testNamespace("itemized-structure");
        long id;
        ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(41, "alpha", "LBL");
        cart.addProperty("KEEP", "yes");

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(name).build()) {
            persistence.archiveAll();
            id = persistence.nextUniquePartId();
            persistence.savePart(id, cart);
        }

        String namespace = "conv:{" + name + "}";
        try (JedisPooled jedis = RedisConnectionFactory.openDefault()) {
            Map<String, String> meta = jedis.hgetAll(namespace + ":part:" + id + ":meta");
            String rawPayload = jedis.get(namespace + ":part:" + id + ":payload");
            String legacyWholeCartPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(serializableConverter.toPersistence(cart));

            assertEquals("PART", meta.get("loadType"));
            assertTrue(meta.get("keyHint").contains("Integer"));
            assertNotNull(meta.get("keyData"));
            assertNotNull(meta.get("labelHint"));
            assertNotNull(meta.get("labelData"));
            assertNotNull(meta.get("valueHint"));
            assertNotNull(meta.get("propertiesHint"));
            assertNotNull(meta.get("propertiesData"));
            assertNotNull(rawPayload);
            assertFalse(rawPayload.equals(legacyWholeCartPayload));
        }
    }

    @Test
    void readsLegacyWholeCartPayloadStructure() throws Exception {
        openRedis().close();

        String name = testNamespace("legacy-whole-cart");
        String namespace = "conv:{" + name + "}";
        long id = 12L;
        ShoppingCart<Integer, String, String> cart =
                new ShoppingCart<>(55, "legacy-cart", "LEGACY", System.currentTimeMillis(), 0);
        String encodedKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(serializableConverter.toPersistence(cart.getKey()));
        String partIndexKey = namespace + ":parts:key:" + encodedKey;

        try (JedisPooled jedis = RedisConnectionFactory.openDefault()) {
            jedis.del(
                    namespace + ":tracker",
                    namespace + ":meta",
                    namespace + ":seq",
                    namespace + ":parts:active",
                    namespace + ":parts:static",
                    namespace + ":parts:expires",
                    namespace + ":completed",
                    namespace + ":part:" + id + ":payload",
                    namespace + ":part:" + id + ":meta",
                    namespace + ":part:" + id + ":keys",
                    partIndexKey
            );
            jedis.sadd(namespace + ":tracker",
                    namespace + ":part:" + id + ":payload",
                    namespace + ":part:" + id + ":meta",
                    namespace + ":part:" + id + ":keys",
                    namespace + ":parts:active",
                    partIndexKey
            );
            jedis.hset(namespace + ":part:" + id + ":meta", Map.of(
                    "id", Long.toString(id),
                    "loadType", cart.getLoadType().name(),
                    "creationTime", Long.toString(cart.getCreationTime()),
                    "expirationTime", Long.toString(cart.getExpirationTime()),
                    "priority", Long.toString(cart.getPriority()),
                    "staticPart", Boolean.toString(false)
            ));
            jedis.set(namespace + ":part:" + id + ":payload",
                    Base64.getUrlEncoder().withoutPadding().encodeToString(serializableConverter.toPersistence(cart)));
            jedis.zadd(namespace + ":parts:active", id, Long.toString(id));
            jedis.sadd(namespace + ":part:" + id + ":keys", encodedKey);
            jedis.zadd(partIndexKey, id, Long.toString(id));
        }

        try (Persistence<Integer> reader = new RedisPersistenceBuilder<Integer>(name).build()) {
            Cart<Integer, ?, String> restored = reader.getPart(id);
            assertNotNull(restored);
            assertEquals(55, restored.getKey());
            assertEquals("legacy-cart", restored.getValue());
            assertEquals("LEGACY", restored.getLabel());
            assertEquals(List.of(id), new ArrayList<>(reader.getAllPartIds(55)));
            assertEquals(List.of("legacy-cart"), reader.getAllParts().stream().map(Cart::getValue).toList());
        }
    }

    @Test
    void supportsEncryptedPayloadsWhileKeepingIndexesReadable() throws Exception {
        openRedis().close();

        String name = testNamespace("encrypted-payload");
        long id;

        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret("redis modern secret")
                .build()) {
            writer.archiveAll();
            id = writer.nextUniquePartId();

            ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(77, "alpha", "PAYLOAD");
            writer.savePart(id, cart);
            writer.saveCompletedBuildKey(77);

            try (JedisPooled jedis = RedisConnectionFactory.openDefault()) {
                String rawPayload = jedis.get("conv:{" + name + "}:part:" + id + ":payload");
                String unencryptedPayload = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(serializableConverter.toPersistence(cart));
                assertNotNull(rawPayload);
                assertFalse(rawPayload.equals(unencryptedPayload));
            }
        }

        try (Persistence<Integer> reader = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret("redis modern secret")
                .build()) {
            assertEquals(List.of(id), new ArrayList<>(reader.getAllPartIds(77)));
            assertEquals(Set.of(77), reader.getCompletedKeys());
            assertEquals("alpha", reader.getPart(id).getValue());
        }

        try (Persistence<Integer> wrongReader = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret("redis wrong secret")
                .build()) {
            assertEquals(List.of(id), new ArrayList<>(wrongReader.getAllPartIds(77)));
            assertThrows(PersistenceException.class, () -> wrongReader.getPart(id));
        }
    }

    @Test
    void modernRedisPayloadEncryptionReadsLegacyDefaultPayloads() throws Exception {
        openRedis().close();

        String name = testNamespace("encrypted-legacy");
        long id;

        try (Persistence<Integer> legacyWriter = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret("redis legacy secret")
                .encryptionAlgorithm("AES")
                .encryptionTransformation("AES/ECB/PKCS5Padding")
                .encryptionKeyLength(16)
                .build()) {
            legacyWriter.archiveAll();
            id = legacyWriter.nextUniquePartId();
            legacyWriter.savePart(id, new ShoppingCart<>(31, "legacy", "PAYLOAD"));
        }

        try (Persistence<Integer> modernReader = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret("redis legacy secret")
                .build()) {
            assertEquals("legacy", modernReader.getPart(id).getValue());
        }
    }

    @Test
    void supportsSecretKeyPayloadEncryption() throws Exception {
        openRedis().close();

        String name = testNamespace("encrypted-secret-key");
        SecretKey key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
        SecretKey wrongKey = new SecretKeySpec("fedcba9876543210".getBytes(StandardCharsets.UTF_8), "AES");
        long id;

        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret(key)
                .build()) {
            writer.archiveAll();
            id = writer.nextUniquePartId();

            ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(91, "secret-key-payload", "PAYLOAD");
            writer.savePart(id, cart);
            writer.saveCompletedBuildKey(91);

            try (JedisPooled jedis = RedisConnectionFactory.openDefault()) {
                String rawPayload = jedis.get("conv:{" + name + "}:part:" + id + ":payload");
                String unencryptedPayload = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(serializableConverter.toPersistence(cart));
                assertNotNull(rawPayload);
                assertFalse(rawPayload.equals(unencryptedPayload));
            }
        }

        try (Persistence<Integer> reader = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret(key)
                .build()) {
            assertEquals(List.of(id), new ArrayList<>(reader.getAllPartIds(91)));
            assertEquals(Set.of(91), reader.getCompletedKeys());
            assertEquals("secret-key-payload", reader.getPart(id).getValue());
        }

        try (Persistence<Integer> wrongReader = new RedisPersistenceBuilder<Integer>(name)
                .encryptionSecret(wrongKey)
                .build()) {
            assertThrows(PersistenceException.class, () -> wrongReader.getPart(id));
        }
    }

    private static void awaitTrue(java.util.function.BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25L);
        }
        assertTrue(condition.getAsBoolean(), message);
    }
}
