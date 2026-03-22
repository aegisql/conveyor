package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.archive.BinaryLogConfiguration;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.utils.CartInputStream;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisArchiverTest {

    @Test
    void deleteArchiverDelegatesToArchiveAccessAcrossAllEntryPoints() {
        RecordingArchiveAccess<Integer> access = new RecordingArchiveAccess<>();
        access.expiredPartIds = List.of(7L, 8L);

        RecordingPersistence persistence = new RecordingPersistence();
        persistence.idsByKey.put(11, List.of(1L, 2L, 2L));

        DeleteRedisArchiver<Integer> archiver = new DeleteRedisArchiver<>(access);
        archiver.setPersistence(persistence);

        archiver.archiveParts(List.of(1L, 1L, 2L));
        archiver.archiveKeys(java.util.Arrays.asList(null, 11));
        archiver.archiveCompleteKeys(List.of(3, 4));
        archiver.archiveExpiredParts();
        archiver.archiveAll();

        assertEquals(List.of(List.of(1L, 2L), List.of(1L, 2L), List.of(7L, 8L)), access.deletedPartsCalls);
        assertEquals(List.of(List.of(3, 4)), access.deletedCompletedKeysCalls);
        assertEquals(1, access.deleteAllCalls);
    }

    @Test
    void persistenceArchiverMovesThenDeletesAcrossSupportedEntryPoints() {
        RecordingArchiveAccess<Integer> access = new RecordingArchiveAccess<>();
        access.expiredPartIds = List.of(3L);

        ShoppingCart<Integer, String, String> cart1 = new ShoppingCart<>(11, "value-1", "L1");
        ShoppingCart<Integer, String, String> cart2 = new ShoppingCart<>(11, "value-2", "L2");
        ShoppingCart<Integer, String, String> cart3 = new ShoppingCart<>(12, "value-3", "L3");
        ShoppingCart<Integer, String, String> staticCart = new ShoppingCart<>(13, "value-4", "L4");

        RecordingPersistence source = new RecordingPersistence();
        source.cartsById.put(1L, rawCart(cart1));
        source.cartsById.put(2L, rawCart(cart2));
        source.cartsById.put(3L, rawCart(cart3));
        source.idsByKey.put(11, List.of(1L, 2L));
        source.allParts.add(rawCart(cart1));
        source.allParts.add(rawCart(cart2));
        source.staticParts.add(rawCart(staticCart));

        RecordingPersistence target = new RecordingPersistence();
        PersistenceRedisArchiver<Integer> archiver = new PersistenceRedisArchiver<>(access, target);
        archiver.setPersistence(source);

        archiver.archiveParts(List.of(1L, 2L));
        archiver.archiveKeys(List.of(11));
        archiver.archiveCompleteKeys(List.of(90));
        archiver.archiveExpiredParts();
        archiver.archiveAll();

        assertEquals(List.of(
                List.of(1L, 2L),
                List.of(1L, 2L),
                List.of(3L)
        ), access.deletedPartsCalls);
        assertEquals(List.of(List.of(90)), access.deletedCompletedKeysCalls);
        assertEquals(1, access.deleteAllCalls);
        assertEquals(8, target.savedCarts.size());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), target.savedIds);
        assertEquals(List.of(cart1, cart2, cart1, cart2, cart3, cart1, cart2, staticCart), target.savedCarts);
    }

    @Test
    void fileArchiverWritesBinaryLogAndDelegatesCleanup() throws Exception {
        RecordingArchiveAccess<Integer> access = new RecordingArchiveAccess<>();
        access.expiredPartIds = List.of(3L);

        ShoppingCart<Integer, String, String> cart1 = new ShoppingCart<>(11, "value-1", "L1");
        ShoppingCart<Integer, String, String> cart2 = new ShoppingCart<>(11, "value-2", "L2");
        ShoppingCart<Integer, String, String> cart3 = new ShoppingCart<>(12, "value-3", "L3");

        RecordingPersistence source = new RecordingPersistence();
        source.cartsById.put(1L, rawCart(cart1));
        source.cartsById.put(2L, rawCart(cart2));
        source.cartsById.put(3L, rawCart(cart3));
        source.idsByKey.put(11, List.of(1L, 2L));

        Path archiveDir = Files.createTempDirectory("redis-archiver-file");
        BinaryLogConfiguration configuration = BinaryLogConfiguration.builder()
                .path(archiveDir.toString())
                .moveToPath(archiveDir.toString())
                .partTableName("redis-file")
                .bucketSize(1)
                .build();

        FileRedisArchiver<Integer> archiver = new FileRedisArchiver<>(access, configuration);
        archiver.setPersistence(source);

        archiver.archiveParts(List.of(1L, 2L));
        archiver.archiveKeys(List.of(11));
        archiver.archiveCompleteKeys(List.of(99));
        archiver.archiveExpiredParts();

        Path archiveFile = Path.of(configuration.getFilePath());
        assertTrue(Files.exists(archiveFile));
        assertTrue(Files.size(archiveFile) > 0L);
        assertEquals(List.of(
                List.of(1L, 2L),
                List.of(1L, 2L),
                List.of(3L)
        ), access.deletedPartsCalls);
        assertEquals(List.of(List.of(99)), access.deletedCompletedKeysCalls);

        List<Cart<Integer, ?, Object>> carts = readCarts(archiveFile);
        assertEquals(5, carts.size());
        assertEquals("value-1", carts.get(0).getValue());
        assertEquals("value-2", carts.get(1).getValue());
        assertEquals("value-1", carts.get(2).getValue());
        assertEquals("value-2", carts.get(3).getValue());
        assertEquals("value-3", carts.get(4).getValue());
    }

    @Test
    void fileArchiverRollsAndZipsWhenConfigured() throws Exception {
        RecordingArchiveAccess<Integer> access = new RecordingArchiveAccess<>();
        String large = "value-".repeat(40);
        ShoppingCart<Integer, String, String> cart1 = new ShoppingCart<>(21, large + "1", "L1");
        ShoppingCart<Integer, String, String> cart2 = new ShoppingCart<>(22, large + "2", "L2");
        ShoppingCart<Integer, String, String> staticCart = new ShoppingCart<>(23, large + "3", "L3");

        RecordingPersistence source = new RecordingPersistence();
        source.allParts.add(rawCart(cart1));
        source.allParts.add(rawCart(cart2));
        source.staticParts.add(rawCart(staticCart));

        Path archiveDir = Files.createTempDirectory("redis-archiver-roll");
        Path movedDir = archiveDir.resolve("rolled");
        BinaryLogConfiguration configuration = BinaryLogConfiguration.builder()
                .path(archiveDir.toString())
                .moveToPath(movedDir.toString())
                .partTableName("redis-roll")
                .maxFileSize("200")
                .zipFile(true)
                .build();

        FileRedisArchiver<Integer> archiver = new FileRedisArchiver<>(access, configuration);
        archiver.setPersistence(source);

        archiver.archiveAll();

        assertEquals(1, access.deleteAllCalls);
        assertTrue(Files.exists(Path.of(configuration.getFilePath())));
        try (var stream = Files.list(movedDir)) {
            assertTrue(stream.anyMatch(path -> path.getFileName().toString().endsWith(".zip")));
        }
    }

    private static List<Cart<Integer, ?, Object>> readCarts(Path archiveFile) throws IOException {
        ArrayList<Cart<Integer, ?, Object>> carts = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(archiveFile.toFile());
             CartInputStream<Integer, Object> cartInputStream = new CartInputStream<>(castCartConverter(new ConverterAdviser<>()), fileInputStream)) {
            Cart<Integer, ?, Object> cart;
            while ((cart = cartInputStream.readCart()) != null) {
                carts.add(cart);
            }
        }
        return carts;
    }

    @SuppressWarnings("unchecked")
    private static CartToBytesConverter<Integer, ?, Object> castCartConverter(ConverterAdviser<?> adviser) {
        return (CartToBytesConverter<Integer, ?, Object>) (CartToBytesConverter<?, ?, ?>) new CartToBytesConverter<>(adviser);
    }

    @SuppressWarnings("unchecked")
    private static Cart<Integer, ?, Object> rawCart(Cart<Integer, ?, ?> cart) {
        return (Cart<Integer, ?, Object>) cart;
    }

    private static final class RecordingArchiveAccess<K> implements RedisArchiveAccess<K> {
        private final ArrayList<List<Long>> deletedPartsCalls = new ArrayList<>();
        private final ArrayList<List<K>> deletedCompletedKeysCalls = new ArrayList<>();
        private Collection<Long> expiredPartIds = List.of();
        private int deleteAllCalls;

        @Override
        public void deleteParts(Collection<Long> ids) {
            deletedPartsCalls.add(List.copyOf(ids));
        }

        @Override
        public void deleteCompletedKeys(Collection<K> keys) {
            deletedCompletedKeysCalls.add(List.copyOf(keys));
        }

        @Override
        public void deleteAll() {
            deleteAllCalls++;
        }

        @Override
        public Collection<Long> expiredPartIds() {
            return expiredPartIds;
        }
    }

    private static final class RecordingPersistence implements Persistence<Integer> {
        private final Map<Long, Cart<Integer, ?, Object>> cartsById = new LinkedHashMap<>();
        private final Map<Integer, List<Long>> idsByKey = new LinkedHashMap<>();
        private final ArrayList<Cart<Integer, ?, Object>> allParts = new ArrayList<>();
        private final ArrayList<Cart<Integer, ?, Object>> staticParts = new ArrayList<>();
        private final ArrayList<Long> savedIds = new ArrayList<>();
        private final ArrayList<Cart<Integer, ?, Object>> savedCarts = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public long nextUniquePartId() {
            return nextId++;
        }

        @Override
        public <L> void savePart(long id, Cart<Integer, ?, L> cart) {
            savedIds.add(id);
            savedCarts.add(castCart(cart));
        }

        @Override
        public <L> boolean isPartPersistent(Cart<Integer, ?, L> cart) {
            return true;
        }

        @Override
        public void savePartId(Integer key, long partId) {
        }

        @Override
        public void saveCompletedBuildKey(Integer key) {
        }

        @Override
        public <L> Collection<Cart<Integer, ?, L>> getParts(Collection<Long> ids) {
            ArrayList<Cart<Integer, ?, L>> result = new ArrayList<>();
            for (Long id : ids) {
                Cart<Integer, ?, Object> cart = cartsById.get(id);
                if (cart != null) {
                    result.add(castCart(cart));
                }
            }
            return result;
        }

        @Override
        public Collection<Long> getAllPartIds(Integer key) {
            return idsByKey.getOrDefault(key, List.of());
        }

        @Override
        public <L> Collection<Cart<Integer, ?, L>> getAllParts() {
            return castCartCollection(allParts);
        }

        @Override
        public <L> Collection<Cart<Integer, ?, L>> getExpiredParts() {
            return List.of();
        }

        @Override
        public <L> Collection<Cart<Integer, ?, L>> getAllStaticParts() {
            return castCartCollection(staticParts);
        }

        @Override
        public Set<Integer> getCompletedKeys() {
            return Set.of();
        }

        @Override
        public void archiveParts(Collection<Long> ids) {
        }

        @Override
        public void archiveKeys(Collection<Integer> keys) {
        }

        @Override
        public void archiveCompleteKeys(Collection<Integer> keys) {
        }

        @Override
        public void archiveExpiredParts() {
        }

        @Override
        public void archiveAll() {
        }

        @Override
        public int getMaxArchiveBatchSize() {
            return 0;
        }

        @Override
        public long getMaxArchiveBatchTime() {
            return 0L;
        }

        @Override
        public long getNumberOfParts() {
            return cartsById.size();
        }

        @Override
        public int getMinCompactSize() {
            return 0;
        }

        @Override
        public Persistence<Integer> copy() {
            return this;
        }

        @Override
        public boolean isPersistentProperty(String property) {
            return true;
        }

        @Override
        public void close() {
        }

        @SuppressWarnings("unchecked")
        private static <L> Cart<Integer, ?, L> castCart(Cart<Integer, ?, ?> cart) {
            return (Cart<Integer, ?, L>) cart;
        }

        @SuppressWarnings("unchecked")
        private static <L> Collection<Cart<Integer, ?, L>> castCartCollection(Collection<? extends Cart<Integer, ?, ?>> carts) {
            return (Collection<Cart<Integer, ?, L>>) carts;
        }
    }
}
