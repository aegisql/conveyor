package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.redis.harness.ThreadPool;
import com.aegisql.conveyor.persistence.redis.harness.Trio;
import com.aegisql.conveyor.persistence.redis.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.redis.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.redis.harness.TrioPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisUiFixtureTest extends RedisTestSupport {

    private static final int FIXTURE_SIZE = 24;
    private static final int STATIC_PART_COUNT = 3;
    private static final String FIXTURE_SECRET = "redis-ui-fixture-secret";

    @Test
    void leavesTrioDataForInspection() throws Exception {
        String name = testNamespace("ui-trio");
        TrioConveyor conveyor = new TrioConveyor();
        ThreadPool pool = new ThreadPool(3);

        System.out.println("Redis UI inspection namespace: " + name);
        System.out.println("Redis UI inspection secret: " + FIXTURE_SECRET);

        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(name)
                .labelConverter(TrioPart.class)
                .encryptionSecret(FIXTURE_SECRET)
                .maxArchiveBatchSize(1)
                .noArchiving()
                .build()) {
            PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> persistent =
                    persistence.wrapConveyor(conveyor);
            persistent.setName("redisUiFixtureTrio");

            try {
                load(
                        persistent,
                        shuffledIds(101L),
                        shuffledIds(202L),
                        shuffledIds(303L),
                        pool
                );
                persistent.completeAndStop().join();
                storeStaticParts(persistence);

                awaitFixtureData(persistence);

                assertEquals(FIXTURE_SIZE, conveyor.results.size());
                assertEquals(FIXTURE_SIZE, conveyor.counter.get());
                assertEquals(FIXTURE_SIZE * 3L + STATIC_PART_COUNT, persistence.getNumberOfParts());
                assertEquals(STATIC_PART_COUNT, persistence.getAllStaticParts().size());
                assertEquals(FIXTURE_SIZE, persistence.getCompletedKeys().size());
            } finally {
                pool.shutdown();
            }
        }
    }

    private static List<Integer> shuffledIds(long seed) {
        List<Integer> ids = new ArrayList<>(FIXTURE_SIZE);
        for (int i = 1; i <= FIXTURE_SIZE; i++) {
            ids.add(i);
        }
        Collections.shuffle(ids, new Random(seed));
        return ids;
    }

    private static void load(
            Conveyor<Integer, SmartLabel<TrioBuilder>, Trio> conveyor,
            List<Integer> text1Ids,
            List<Integer> text2Ids,
            List<Integer> numberIds,
            ThreadPool pool) {
        AtomicReference<CompletableFuture<Boolean>> f1Ref = new AtomicReference<>();
        AtomicReference<CompletableFuture<Boolean>> f2Ref = new AtomicReference<>();
        AtomicReference<CompletableFuture<Boolean>> f3Ref = new AtomicReference<>();

        CompletableFuture<Void> t1 = pool.runAsync(() -> text1Ids.forEach(key ->
                f1Ref.set(conveyor.part().id(key).label(TrioPart.TEXT1).value("txt1_" + key).place())));
        CompletableFuture<Void> t2 = pool.runAsync(() -> text2Ids.forEach(key ->
                f2Ref.set(conveyor.part().id(key).label(TrioPart.TEXT2).value("txt2_" + key).place())));
        CompletableFuture<Void> t3 = pool.runAsync(() -> numberIds.forEach(key ->
                f3Ref.set(conveyor.part().id(key).label(TrioPart.NUMBER).value(key).place())));

        CompletableFuture.allOf(t1, t2, t3).join();
        f1Ref.get().join();
        f2Ref.get().join();
        f3Ref.get().join();
    }

    private static void storeStaticParts(Persistence<Integer> persistence) {
        long now = System.currentTimeMillis();
        persistence.savePart(
                persistence.nextUniquePartId(),
                new ShoppingCart<>(null, "fixture-static-title", TrioPart.TEXT1, now, 0, null, LoadType.STATIC_PART, 10)
        );
        persistence.savePart(
                persistence.nextUniquePartId(),
                new ShoppingCart<>(null, "fixture-static-subtitle", TrioPart.TEXT2, now, 0, null, LoadType.STATIC_PART, 20)
        );
        persistence.savePart(
                persistence.nextUniquePartId(),
                new ShoppingCart<>(null, 4242, TrioPart.NUMBER, now, 0, null, LoadType.STATIC_PART, 30)
        );
    }

    private static void awaitFixtureData(Persistence<Integer> persistence) throws Exception {
        long expectedPartCount = FIXTURE_SIZE * 3L + STATIC_PART_COUNT;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (persistence.getNumberOfParts() == expectedPartCount
                    && persistence.getCompletedKeys().size() == FIXTURE_SIZE
                    && persistence.getAllStaticParts().size() == STATIC_PART_COUNT) {
                return;
            }
            Thread.sleep(50L);
        }
    }
}
