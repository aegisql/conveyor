package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.Acknowledge;
import com.aegisql.conveyor.AcknowledgeStatus;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.cart.ResultConsumerCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.SerializableToBytesConverter;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.utils.CartInputStream;
import com.aegisql.conveyor.serial.SerializablePredicate;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    void bootstrapsLuaScriptMetadataAndReloadsAfterScriptFlush() throws Exception {
        openRedis().close();

        String name = testNamespace("lua-bootstrap");
        String namespaceMetaKey = "conv:{" + name + "}:meta";
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(name).build();
             JedisPooled jedis = openRedis()) {
            persistence.archiveAll();

            long firstId = persistence.nextUniquePartId();
            persistence.savePart(firstId, new ShoppingCart<>(1, "first", "L1"));

            Map<String, String> namespaceMeta = jedis.hgetAll(namespaceMetaKey);
            assertEquals(RedisPersistence.BACKEND_NAME, namespaceMeta.get("backend"));
            assertEquals(RedisPersistence.BACKEND_VERSION, namespaceMeta.get("version"));
            assertEquals(name, namespaceMeta.get("name"));
            assertEquals(RedisLuaScriptBundle.SCRIPT_MODE, namespaceMeta.get("scriptMode"));
            assertEquals(RedisLuaScriptBundle.BUNDLE_VERSION, namespaceMeta.get("scriptBundleVersion"));

            jedis.scriptFlush();

            long secondId = persistence.nextUniquePartId();
            persistence.savePart(secondId, new ShoppingCart<>(2, "second", "L2"));

            assertEquals(
                    List.of("first", "second"),
                    persistence.getAllParts().stream().map(Cart::getValue).toList()
            );
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
    void supportsNoActionArchiveStrategy() throws Exception {
        openRedis().close();

        String name = testNamespace("no-action-archive");
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(name).noArchiving().build()) {
            long now = System.currentTimeMillis();
            persistence.archiveAll();

            long activeId = persistence.nextUniquePartId();
            long expiredId = persistence.nextUniquePartId();
            persistence.savePart(activeId, new ShoppingCart<>(11, "active", "A"));
            persistence.savePart(expiredId, new ShoppingCart<>(12, "expired", "E", now, now - 10, null, LoadType.PART, 0));
            persistence.saveCompletedBuildKey(11);

            persistence.archiveParts(List.of(activeId));
            persistence.archiveCompleteKeys(Set.of(11));
            persistence.archiveExpiredParts();
            persistence.archiveAll();

            assertNotNull(persistence.getPart(activeId));
            assertNotNull(persistence.getPart(expiredId));
            assertEquals(Set.of(11), persistence.getCompletedKeys());
            assertEquals(2L, persistence.getNumberOfParts());
        } finally {
            try (Persistence<Integer> cleanup = new RedisPersistenceBuilder<Integer>(name).build()) {
                cleanup.archiveAll();
            }
        }
    }

    @Test
    void movesArchivedDataToAnotherPersistence() throws Exception {
        openRedis().close();

        String sourceName = testNamespace("move-to-persistence-source");
        String targetName = testNamespace("move-to-persistence-target");

        try (Persistence<Integer> target = new RedisPersistenceBuilder<Integer>(targetName).build();
             Persistence<Integer> source = new RedisPersistenceBuilder<Integer>(sourceName).archiver(target).build()) {
            source.archiveAll();
            target.archiveAll();

            long activeId = source.nextUniquePartId();
            long staticId = source.nextUniquePartId();
            source.savePart(activeId, new ShoppingCart<>(21, "active-value", "ACTIVE"));
            source.savePart(staticId, new ShoppingCart<>(null, "static-value", "STATIC",
                    System.currentTimeMillis(), 0, null, LoadType.STATIC_PART, 0));
            source.saveCompletedBuildKey(21);

            source.archiveAll();

            assertTrue(source.getAllParts().isEmpty());
            assertTrue(source.getAllStaticParts().isEmpty());
            assertTrue(source.getCompletedKeys().isEmpty());
            assertEquals(0L, source.getNumberOfParts());

            assertEquals(List.of("active-value"), target.getAllParts().stream().map(Cart::getValue).toList());
            assertEquals(List.of("static-value"), target.getAllStaticParts().stream().map(Cart::getValue).toList());
            assertTrue(target.getCompletedKeys().isEmpty());
            assertEquals(2L, target.getNumberOfParts());
        }
    }

    @Test
    void movesArchivedDataToBinaryLogFile() throws Exception {
        openRedis().close();

        String name = testNamespace("move-to-file");
        Path archiveDir = Path.of("target", "redis-archive-tests", name);
        BinaryLogConfiguration configuration = BinaryLogConfiguration.builder()
                .path(archiveDir.toString())
                .moveToPath(archiveDir.toString())
                .partTableName("redis-file")
                .maxFileSize("10MB")
                .build();

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(name).archiver(configuration).build()) {
            persistence.archiveAll();
            persistence.savePart(persistence.nextUniquePartId(), new ShoppingCart<>(31, "first-file-value", "F1"));
            persistence.savePart(persistence.nextUniquePartId(), new ShoppingCart<>(32, "second-file-value", "F2"));
            persistence.saveCompletedBuildKey(31);

            persistence.archiveAll();

            assertTrue(persistence.getAllParts().isEmpty());
            assertTrue(persistence.getCompletedKeys().isEmpty());
            assertEquals(0L, persistence.getNumberOfParts());
        }

        Path archiveFile = archiveDir.resolve("redis-file.blog");
        assertTrue(Files.exists(archiveFile));
        assertTrue(Files.size(archiveFile) > 0L);

        ArrayList<Cart<Integer, ?, Object>> carts = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(archiveFile.toFile());
             CartInputStream<Integer, Object> cartInputStream =
                     new CartInputStream<>(castCartConverter(new ConverterAdviser<>()), fileInputStream)) {
            Cart<Integer, ?, Object> cart;
            while ((cart = cartInputStream.readCart()) != null) {
                carts.add(cart);
            }
        }

        assertEquals(List.of("first-file-value", "second-file-value"), carts.stream().map(Cart::getValue).toList());
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
    void replaysIncompleteBuildsAcrossPersistentConveyorRestart() throws Exception {
        openRedis().close();

        String name = testNamespace("persistent-replay");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name)) {
            cleanup.archiveAll();
        }

        Map<Integer, String> firstResults = new ConcurrentHashMap<>();
        Persistence<Integer> firstPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> first =
                firstPersistence.wrapConveyor(newRecoveryConveyor("redis-replay-first", firstResults, null, false));
        try {
            first.part().id(1).label(RecoveryPart.LEFT).value("alpha").place().join();
            first.part().id(1).label(RecoveryPart.RIGHT).value("beta").place().join();
        } finally {
            first.stop();
        }

        assertTrue(firstResults.isEmpty(), "The first conveyor should stop before the build is complete");

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        Persistence<Integer> secondPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> second =
                secondPersistence.wrapConveyor(newRecoveryConveyor("redis-replay-second", recoveredResults, null, true));
        try {
            second.part().id(1).label(RecoveryPart.NUMBER).value(7).place().join();
            awaitTrue(() -> "no-prefix:alpha:beta:7".equals(recoveredResults.get(1)),
                    "Recovered conveyor should complete the replayed build when the missing part arrives");
        } finally {
            second.completeAndStop().join();
        }

        try (Persistence<Integer> inspector = newRecoveryPersistence(name)) {
            awaitTrue(() -> inspector.getAllPartIds(1).isEmpty()
                            && inspector.getAllParts().isEmpty()
                            && inspector.getCompletedKeys().isEmpty()
                            && inspector.getAllStaticParts().isEmpty(),
                    "Recovered auto-ack build should eventually drain its Redis state after completion");
        }
    }

    @Test
    void recoveredBuildProvidesExplicitAcknowledgeHandle() throws Exception {
        openRedis().close();

        String name = testNamespace("persistent-ack");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name)) {
            cleanup.archiveAll();
        }

        Persistence<Integer> firstPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> first =
                firstPersistence.wrapConveyor(newRecoveryConveyor("redis-ack-first", new ConcurrentHashMap<>(), null, false));
        try {
            first.part().id(3).label(RecoveryPart.LEFT).value("left").place().join();
            first.part().id(3).label(RecoveryPart.RIGHT).value("right").place().join();
        } finally {
            first.stop();
        }

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        AtomicReference<Acknowledge> acknowledgeRef = new AtomicReference<>();
        Persistence<Integer> secondPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> second =
                secondPersistence.wrapConveyor(newRecoveryConveyor("redis-ack-second", recoveredResults, acknowledgeRef, false));
        boolean stopped = false;
        try (Persistence<Integer> inspector = newRecoveryPersistence(name)) {
            second.part().id(3).label(RecoveryPart.NUMBER).value(9).place().join();

            awaitTrue(() -> "no-prefix:left:right:9".equals(recoveredResults.get(3)),
                    "Recovered build should complete once the missing part is supplied");
            awaitTrue(() -> acknowledgeRef.get() != null,
                    "Recovered completion should expose an acknowledge handle when auto-acknowledge is disabled");
            assertFalse(acknowledgeRef.get().isAcknowledged(),
                    "Recovered build should remain unacknowledged until the test calls ack()");
            awaitTrue(() -> inspector.getNumberOfParts() > 0,
                    "Recovered Redis state should remain populated while the acknowledge handle is still pending");

            acknowledgeRef.get().ack();
            assertTrue(acknowledgeRef.get().isAcknowledged(),
                    "Calling ack() should update the recovered acknowledge handle");
            second.completeAndStop().join();
            stopped = true;

            awaitTrue(() -> inspector.getCompletedKeys().isEmpty()
                            && inspector.getAllParts().isEmpty()
                            && inspector.getAllStaticParts().isEmpty()
                            && inspector.getNumberOfParts() == 0,
                    "Explicit acknowledge followed by conveyor drain should cleanup recovered Redis persistence state");
        } finally {
            if (!stopped) {
                second.stop();
            }
        }
    }

    @Test
    void recoveredBuildCanceledStatusCleansRedisState() throws Exception {
        openRedis().close();

        String name = testNamespace("persistent-cancel-cleanup");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name)) {
            cleanup.archiveAll();
        }

        Persistence<Integer> firstPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> first =
                firstPersistence.wrapConveyor(newRecoveryConveyor("redis-cancel-first", new ConcurrentHashMap<>(), null, false));
        try {
            first.part().id(5).label(RecoveryPart.LEFT).value("cancel-left").place().join();
            first.part().id(5).label(RecoveryPart.RIGHT).value("cancel-right").place().join();
        } finally {
            first.stop();
        }

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> recoveredForward =
                newRecoveryConveyor("redis-cancel-second", recoveredResults, null, null, Status.CANCELED);
        Persistence<Integer> secondPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> second =
                secondPersistence.wrapConveyor(recoveredForward);
        try {
            second.command().id(5).cancel().join();
            awaitTrue(() -> recoveredForward.getCollectorSize() == 0,
                    "Recovered canceled build should leave the working conveyor");
            assertTrue(recoveredResults.isEmpty(), "Canceled recovered build should not produce a READY result");
        } finally {
            second.completeAndStop().join();
        }

        try (Persistence<Integer> inspector = newRecoveryPersistence(name)) {
            awaitTrue(() -> inspector.getAllPartIds(5).isEmpty()
                            && inspector.getAllParts().isEmpty()
                            && inspector.getCompletedKeys().isEmpty()
                            && inspector.getAllStaticParts().isEmpty(),
                    "Recovered canceled build should cleanup Redis persistence state");
        }
    }

    @Test
    void recoveredBuildTimedOutStatusCleansRedisState() throws Exception {
        openRedis().close();

        String name = testNamespace("persistent-timeout-cleanup");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name)) {
            cleanup.archiveAll();
        }

        Persistence<Integer> firstPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> first =
                firstPersistence.wrapConveyor(newRecoveryConveyor("redis-timeout-first", new ConcurrentHashMap<>(), null, false));
        try {
            first.part().id(7).label(RecoveryPart.LEFT).value("timeout-left").place().join();
            first.part().id(7).label(RecoveryPart.RIGHT).value("timeout-right").place().join();
        } finally {
            first.stop();
        }

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        List<AcknowledgeStatus<Integer>> statuses = Collections.synchronizedList(new ArrayList<>());
        List<ScrapBin<Integer, ?>> scraps = Collections.synchronizedList(new ArrayList<>());
        AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> recoveredForward =
                newRecoveryConveyor("redis-timeout-second", recoveredResults, null, Duration.ofMillis(250), Status.TIMED_OUT);
        recoveredForward.addBeforeKeyEvictionAction(statuses::add);
        recoveredForward.scrapConsumer(scrap -> scraps.add((ScrapBin<Integer, ?>) scrap)).set();

        Persistence<Integer> secondPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> second =
                secondPersistence.wrapConveyor(recoveredForward);
        try {
            awaitTrue(() -> statuses.stream().anyMatch(status -> status.getKey() == 7 && status.getStatus() == Status.TIMED_OUT),
                    "Recovered incomplete build should eventually expire with TIMED_OUT status");
            awaitTrue(() -> scraps.stream().anyMatch(scrap ->
                            scrap.key.equals(7)
                                    && scrap.failureType == ScrapBin.FailureType.BUILD_EXPIRED
                                    && scrap.comment.startsWith("Site expired. No timeout action")),
                    "Recovered timed out build should publish BUILD_EXPIRED scrap without a timeout action");
            assertTrue(recoveredResults.isEmpty(), "Recovered timed out build should not produce a READY result");
        } finally {
            second.completeAndStop().join();
        }

        try (Persistence<Integer> inspector = newRecoveryPersistence(name)) {
            awaitTrue(() -> inspector.getAllPartIds(7).isEmpty()
                            && inspector.getAllParts().isEmpty()
                            && inspector.getCompletedKeys().isEmpty()
                            && inspector.getAllStaticParts().isEmpty(),
                    "Recovered timed out build should cleanup Redis persistence state after the conveyor drains cleanly");
        }
    }

    @Test
    void recoveredBuildInvalidStatusCleansRedisStateWhenTimeoutActionFails() throws Exception {
        openRedis().close();

        String name = testNamespace("persistent-invalid-cleanup");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name)) {
            cleanup.archiveAll();
        }

        Persistence<Integer> firstPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> first =
                firstPersistence.wrapConveyor(newRecoveryConveyor("redis-invalid-first", new ConcurrentHashMap<>(), null, false));
        try {
            first.part().id(8).label(RecoveryPart.LEFT).value("invalid-left").place().join();
            first.part().id(8).label(RecoveryPart.RIGHT).value("invalid-right").place().join();
        } finally {
            first.stop();
        }

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        List<AcknowledgeStatus<Integer>> statuses = Collections.synchronizedList(new ArrayList<>());
        List<ScrapBin<Integer, ?>> scraps = Collections.synchronizedList(new ArrayList<>());
        AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> recoveredForward =
                newRecoveryConveyor(
                        "redis-invalid-second",
                        recoveredResults,
                        null,
                        Duration.ofMillis(250),
                        supplier -> {
                            throw new IllegalStateException("recovered timeout failure");
                        },
                        Status.INVALID);
        recoveredForward.addBeforeKeyEvictionAction(statuses::add);
        recoveredForward.scrapConsumer(scrap -> scraps.add((ScrapBin<Integer, ?>) scrap)).set();

        Persistence<Integer> secondPersistence = newRecoveryPersistence(name);
        PersistentConveyor<Integer, SmartLabel<RecoveryBuilder>, String> second =
                secondPersistence.wrapConveyor(recoveredForward);
        try {
            awaitTrue(() -> statuses.stream().anyMatch(status -> status.getKey() == 8 && status.getStatus() == Status.INVALID),
                    "Recovered timeout-action failure should eventually mark the build INVALID");
            awaitTrue(() -> scraps.stream().anyMatch(scrap ->
                            scrap.key.equals(8)
                                    && scrap.failureType == ScrapBin.FailureType.BUILD_EXPIRED
                                    && scrap.comment.startsWith("Timeout processor failed ")
                                    && scrap.error instanceof IllegalStateException),
                    "Recovered timeout-action failure should publish BUILD_EXPIRED scrap with the thrown error");
            assertTrue(recoveredResults.isEmpty(), "Recovered invalid build should not produce a READY result");
        } finally {
            second.completeAndStop().join();
        }

        try (Persistence<Integer> inspector = newRecoveryPersistence(name)) {
            awaitTrue(() -> inspector.getAllPartIds(8).isEmpty()
                            && inspector.getAllParts().isEmpty()
                            && inspector.getCompletedKeys().isEmpty()
                            && inspector.getAllStaticParts().isEmpty(),
                    "Recovered invalid build should cleanup Redis persistence state after the conveyor drains cleanly");
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
    void supportsExplicitRestoreOrderPoliciesForRecoveryFacingReads() throws Exception {
        openRedis().close();

        String name = testNamespace("restore-order");
        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name).build()) {
            writer.archiveAll();

            long now = System.currentTimeMillis();
            writer.savePart(30L, new ShoppingCart<>(7, "third", "A30", now, 0, null, LoadType.PART, 500));
            writer.savePart(10L, new ShoppingCart<>(7, "first", "A10", now, 0, null, LoadType.PART, -100));
            writer.savePart(20L, new ShoppingCart<>(7, "second", "A20", now, 0, null, LoadType.PART, 999));

            writer.savePart(40L, new ShoppingCart<>(null, "static-fourth", "S40", now, 0, null, LoadType.STATIC_PART, 700));
            writer.savePart(5L, new ShoppingCart<>(null, "static-first", "S05", now, 0, null, LoadType.STATIC_PART, -10));
            writer.savePart(25L, new ShoppingCart<>(null, "static-second", "S25", now, 0, null, LoadType.STATIC_PART, 0));
        }

        try (Persistence<Integer> byId = new RedisPersistenceBuilder<Integer>(name)
                .restoreOrder(RestoreOrder.BY_ID)
                .build()) {
            assertEquals(List.of(10L, 20L, 30L), new ArrayList<>(byId.getAllPartIds(7)));
            assertEquals(
                    List.of("first", "second", "third"),
                    byId.getAllParts().stream().map(Cart::getValue).toList()
            );
            assertEquals(
                    List.of("static-first", "static-second", "static-fourth"),
                    byId.getAllStaticParts().stream().map(Cart::getValue).toList()
            );
        }

        try (Persistence<Integer> byPriority = new RedisPersistenceBuilder<Integer>(name)
                .restoreOrder(RestoreOrder.BY_PRIORITY_AND_ID)
                .build()) {
            assertEquals(List.of(20L, 30L, 10L), new ArrayList<>(byPriority.getAllPartIds(7)));
            assertEquals(
                    List.of("second", "third", "first"),
                    byPriority.getAllParts().stream().map(Cart::getValue).toList()
            );
            assertEquals(
                    List.of("static-fourth", "static-second", "static-first"),
                    byPriority.getAllStaticParts().stream().map(Cart::getValue).toList()
            );
        }
    }

    @Test
    void supportsNoOrderByIdAndPriorityPoliciesForExpiredReads() throws Exception {
        openRedis().close();

        String name = testNamespace("restore-order-expired");
        long now = System.currentTimeMillis();
        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name).build()) {
            writer.archiveAll();
            writer.savePart(30L, new ShoppingCart<>(9, "expire-first", "E30", now, now - 300, null, LoadType.PART, 5));
            writer.savePart(10L, new ShoppingCart<>(9, "expire-third", "E10", now, now - 100, null, LoadType.PART, 100));
            writer.savePart(20L, new ShoppingCart<>(9, "expire-second", "E20", now, now - 200, null, LoadType.PART, -5));
        }

        try (Persistence<Integer> noOrder = new RedisPersistenceBuilder<Integer>(name)
                .restoreOrder(RestoreOrder.NO_ORDER)
                .build()) {
            assertEquals(
                    List.of("expire-first", "expire-second", "expire-third"),
                    noOrder.getExpiredParts().stream().map(Cart::getValue).toList()
            );
        }

        try (Persistence<Integer> byId = new RedisPersistenceBuilder<Integer>(name)
                .restoreOrder(RestoreOrder.BY_ID)
                .build()) {
            assertEquals(
                    List.of("expire-third", "expire-second", "expire-first"),
                    byId.getExpiredParts().stream().map(Cart::getValue).toList()
            );
        }

        try (Persistence<Integer> byPriority = new RedisPersistenceBuilder<Integer>(name)
                .restoreOrder(RestoreOrder.BY_PRIORITY_AND_ID)
                .build()) {
            assertEquals(
                    List.of("expire-third", "expire-first", "expire-second"),
                    byPriority.getExpiredParts().stream().map(Cart::getValue).toList()
            );
        }
    }

    @Test
    void replaysRecoveredPartsInConfiguredPriorityOrder() throws Exception {
        openRedis().close();

        String name = testNamespace("recovery-restore-order");
        try (Persistence<Integer> cleanup = newRecoveryPersistence(name, RestoreOrder.BY_PRIORITY_AND_ID)) {
            cleanup.archiveAll();
        }

        Map<Integer, String> firstResults = new ConcurrentHashMap<>();
        Persistence<Integer> firstPersistence = newRecoveryPersistence(name, RestoreOrder.BY_PRIORITY_AND_ID);
        PersistentConveyor<Integer, SmartLabel<OrderedReplayBuilder>, String> first =
                firstPersistence.wrapConveyor(newOrderedReplayConveyor("redis-ordered-replay-first", firstResults));
        try {
            first.part().id(11).label(OrderedReplayPart.STEP).value("low").priority(1).place().join();
            first.part().id(11).label(OrderedReplayPart.STEP).value("high").priority(50).place().join();
        } finally {
            first.stop();
        }

        assertTrue(firstResults.isEmpty(), "The first conveyor should stop before the ordered build is complete");

        Map<Integer, String> recoveredResults = new ConcurrentHashMap<>();
        Persistence<Integer> secondPersistence = newRecoveryPersistence(name, RestoreOrder.BY_PRIORITY_AND_ID);
        PersistentConveyor<Integer, SmartLabel<OrderedReplayBuilder>, String> second =
                secondPersistence.wrapConveyor(newOrderedReplayConveyor("redis-ordered-replay-second", recoveredResults));
        try {
            second.part().id(11).label(OrderedReplayPart.STEP).value("new").priority(0).place().join();
            awaitTrue(() -> "high>low>new".equals(recoveredResults.get(11)),
                    "Recovered conveyor should replay higher-priority persisted carts before lower-priority ones");
        } finally {
            second.completeAndStop().join();
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
            assertNull(meta.get("valueData"));
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
    void readsLegacyItemizedPayloadStructureWithMirroredValueData() throws Exception {
        openRedis().close();

        String name = testNamespace("legacy-itemized-value");
        String namespace = "conv:{" + name + "}";
        long id;
        ShoppingCart<Integer, String, String> cart =
                new ShoppingCart<>(77, "mirrored-value", "MIRROR", System.currentTimeMillis(), 0);

        try (Persistence<Integer> writer = new RedisPersistenceBuilder<Integer>(name).build()) {
            writer.archiveAll();
            id = writer.nextUniquePartId();
            writer.savePart(id, cart);
        }

        try (JedisPooled jedis = RedisConnectionFactory.openDefault()) {
            String metaKey = namespace + ":part:" + id + ":meta";
            String payloadKey = namespace + ":part:" + id + ":payload";
            String rawPayload = jedis.get(payloadKey);
            assertNotNull(rawPayload);

            jedis.hset(metaKey, "valueData", rawPayload);
            jedis.del(payloadKey);
        }

        try (Persistence<Integer> reader = new RedisPersistenceBuilder<Integer>(name).build()) {
            Cart<Integer, ?, String> restored = reader.getPart(id);
            assertNotNull(restored);
            assertEquals(77, restored.getKey());
            assertEquals("mirrored-value", restored.getValue());
            assertEquals("MIRROR", restored.getLabel());
            assertEquals(List.of(id), new ArrayList<>(reader.getAllPartIds(77)));
            assertEquals(List.of("mirrored-value"), reader.getAllParts().stream().map(Cart::getValue).toList());
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

    private static AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> newRecoveryConveyor(
            String name,
            Map<Integer, String> results,
            AtomicReference<Acknowledge> acknowledgeRef,
            boolean autoAcknowledge) {
        return newRecoveryConveyor(name, results, acknowledgeRef, null, autoAcknowledge ? new Status[]{Status.READY} : new Status[0]);
    }

    private static AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> newRecoveryConveyor(
            String name,
            Map<Integer, String> results,
            AtomicReference<Acknowledge> acknowledgeRef,
            Duration builderTimeout,
            Status... autoAcknowledgeStatuses) {
        return newRecoveryConveyor(name, results, acknowledgeRef, builderTimeout, null, autoAcknowledgeStatuses);
    }

    private static AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> newRecoveryConveyor(
            String name,
            Map<Integer, String> results,
            AtomicReference<Acknowledge> acknowledgeRef,
            Duration builderTimeout,
            Consumer<Supplier<? extends String>> timeoutAction,
            Status... autoAcknowledgeStatuses) {
        AssemblingConveyor<Integer, SmartLabel<RecoveryBuilder>, String> conveyor = new AssemblingConveyor<>();
        conveyor.setName(name);
        conveyor.setBuilderSupplier(RecoveryBuilder::new);
        if (builderTimeout != null) {
            conveyor.setDefaultBuilderTimeout(builderTimeout);
        }
        if (timeoutAction != null) {
            conveyor.setOnTimeoutAction(timeoutAction);
        }
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor)
                .accepted(RecoveryPart.LEFT, RecoveryPart.RIGHT, RecoveryPart.NUMBER));
        if (autoAcknowledgeStatuses != null && autoAcknowledgeStatuses.length > 0) {
            conveyor.autoAcknowledgeOnStatus(autoAcknowledgeStatuses[0], java.util.Arrays.copyOfRange(autoAcknowledgeStatuses, 1, autoAcknowledgeStatuses.length));
        } else {
            conveyor.setAutoAcknowledge(false);
        }
        conveyor.resultConsumer(bin -> {
            results.put(bin.key, bin.product);
            if (acknowledgeRef != null) {
                acknowledgeRef.set(bin.acknowledge);
            }
        }).set();
        return conveyor;
    }

    private static Persistence<Integer> newRecoveryPersistence(String name) {
        return new RedisPersistenceBuilder<Integer>(name)
                .maxArchiveBatchSize(4)
                .maxArchiveBatchTime(100L)
                .build();
    }

    private static Persistence<Integer> newRecoveryPersistence(String name, RestoreOrder restoreOrder) {
        return new RedisPersistenceBuilder<Integer>(name)
                .maxArchiveBatchSize(4)
                .maxArchiveBatchTime(100L)
                .restoreOrder(restoreOrder)
                .build();
    }

    private static AssemblingConveyor<Integer, SmartLabel<OrderedReplayBuilder>, String> newOrderedReplayConveyor(
            String name,
            Map<Integer, String> results) {
        AssemblingConveyor<Integer, SmartLabel<OrderedReplayBuilder>, String> conveyor = new AssemblingConveyor<>();
        conveyor.setName(name);
        conveyor.setBuilderSupplier(OrderedReplayBuilder::new);
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(3));
        conveyor.autoAcknowledgeOnStatus(Status.READY);
        conveyor.resultConsumer(bin -> results.put(bin.key, bin.product)).set();
        return conveyor;
    }

    private static final class RecoveryBuilder implements Supplier<String>, Serializable {
        private String prefix = "no-prefix";
        private String left;
        private String right;
        private Integer number;

        @Override
        public String get() {
            return prefix + ":" + left + ":" + right + ":" + number;
        }

        static void setPrefix(RecoveryBuilder builder, String prefix) {
            builder.prefix = prefix;
        }

        static void setLeft(RecoveryBuilder builder, String left) {
            builder.left = left;
        }

        static void setRight(RecoveryBuilder builder, String right) {
            builder.right = right;
        }

        static void setNumber(RecoveryBuilder builder, Integer number) {
            builder.number = number;
        }
    }

    private enum RecoveryPart implements SmartLabel<RecoveryBuilder> {
        PREFIX(SmartLabel.of(RecoveryBuilder::setPrefix)),
        LEFT(SmartLabel.of(RecoveryBuilder::setLeft)),
        RIGHT(SmartLabel.of(RecoveryBuilder::setRight)),
        NUMBER(SmartLabel.of(RecoveryBuilder::setNumber));

        private final SmartLabel<RecoveryBuilder> inner;

        RecoveryPart(SmartLabel<RecoveryBuilder> inner) {
            this.inner = inner;
        }

        @Override
        public BiConsumer<RecoveryBuilder, Object> get() {
            return inner.get();
        }
    }

    private static final class OrderedReplayBuilder implements Supplier<String>, Serializable {
        private final List<String> steps = new ArrayList<>();

        @Override
        public String get() {
            return String.join(">", steps);
        }

        static void addStep(OrderedReplayBuilder builder, String step) {
            builder.steps.add(step);
        }
    }

    private enum OrderedReplayPart implements SmartLabel<OrderedReplayBuilder> {
        STEP(SmartLabel.of(OrderedReplayBuilder::addStep));

        private final SmartLabel<OrderedReplayBuilder> inner;

        OrderedReplayPart(SmartLabel<OrderedReplayBuilder> inner) {
            this.inner = inner;
        }

        @Override
        public BiConsumer<OrderedReplayBuilder, Object> get() {
            return inner.get();
        }
    }

    @SuppressWarnings("unchecked")
    private static CartToBytesConverter<Integer, ?, Object> castCartConverter(ConverterAdviser<?> adviser) {
        return (CartToBytesConverter<Integer, ?, Object>) (CartToBytesConverter<?, ?, ?>) new CartToBytesConverter<>(adviser);
    }
}
