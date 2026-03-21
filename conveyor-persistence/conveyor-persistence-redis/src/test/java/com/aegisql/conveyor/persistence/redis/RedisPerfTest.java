package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.redis.harness.ThreadPool;
import com.aegisql.conveyor.persistence.redis.harness.Trio;
import com.aegisql.conveyor.persistence.redis.harness.TrioBuilder;
import com.aegisql.conveyor.persistence.redis.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.redis.harness.TrioPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisPerfTest extends RedisTestSupport {

    private ThreadPool pool;
    private int testSize;
    private int batchSize;
    private int sleepNumber;
    private final double sleepTime = 0.01d;

    @BeforeEach
    void setUp() {
        openRedis().close();
        pool = new ThreadPool(3);
        testSize = getPerfTestSize();
        batchSize = Math.max(1, testSize / 20);
        sleepNumber = batchSize;
        System.out.println("--- RedisPerfTest " + new Date() + " size=" + testSize);
    }

    @AfterEach
    void tearDown() {
        pool.shutdown();
    }

    @Test
    void testPersistentConveyorShuffled() throws Exception {
        TrioConveyor conveyor = new TrioConveyor();

        try (Persistence<Integer> persistence = newPersistence("perf-shuffled")) {
            persistence.archiveAll();

            PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> persistent =
                    persistence.wrapConveyor(conveyor);
            persistent.setName("redisPerfShuffled");

            List<Integer> t1 = shuffledIds();
            List<Integer> t2 = shuffledIds();
            List<Integer> t3 = shuffledIds();

            long start = System.currentTimeMillis();
            try {
                load(persistent, t1, t2, t3);
                long loadComplete = System.currentTimeMillis();
                System.out.println("testPersistentConveyorShuffled load complete in " + (loadComplete - start) + " msec.");

                try (Persistence<Integer> inspector = persistence.copy()) {
                    waitUntilArchived(inspector, testSize);
                }

                long archived = System.currentTimeMillis();
                System.out.println("testPersistentConveyorShuffled data loaded and archived in " + (archived - start) + " msec.");
                assertEquals(testSize, conveyor.results.size());
                assertEquals(testSize, conveyor.counter.get());
            } finally {
                persistent.completeAndStop().join();
            }
        }
    }

    @Test
    void testPersistentConveyorSorted() throws Exception {
        TrioConveyor conveyor = new TrioConveyor();

        try (Persistence<Integer> persistence = newPersistence("perf-sorted")) {
            persistence.archiveAll();

            PersistentConveyor<Integer, SmartLabel<TrioBuilder>, Trio> persistent =
                    persistence.wrapConveyor(conveyor);
            persistent.setName("redisPerfSorted");

            List<Integer> t1 = orderedIds();
            List<Integer> t2 = orderedIds();
            List<Integer> t3 = orderedIds();

            long start = System.currentTimeMillis();
            try {
                load(persistent, t1, t2, t3);
                long loadComplete = System.currentTimeMillis();
                System.out.println("testPersistentConveyorSorted load complete in " + (loadComplete - start) + " msec.");

                try (Persistence<Integer> inspector = persistence.copy()) {
                    waitUntilArchived(inspector, testSize);
                }

                long archived = System.currentTimeMillis();
                System.out.println("testPersistentConveyorSorted data loaded and archived in " + (archived - start) + " msec.");
                assertEquals(testSize, conveyor.results.size());
                assertEquals(testSize, conveyor.counter.get());
            } finally {
                persistent.completeAndStop().join();
            }
        }
    }

    @Test
    void testOnlyConveyor() {
        TrioConveyor conveyor = new TrioConveyor();
        List<Integer> t1 = shuffledIds();
        List<Integer> t2 = shuffledIds();
        List<Integer> t3 = shuffledIds();

        long start = System.currentTimeMillis();
        load(conveyor, t1, t2, t3);
        long loadComplete = System.currentTimeMillis();

        System.out.println("testOnlyConveyor load complete in " + (loadComplete - start) + " msec.");
        assertEquals(testSize, conveyor.results.size());
        assertEquals(testSize, conveyor.counter.get());
    }

    private List<Integer> orderedIds() {
        List<Integer> ids = new ArrayList<>(testSize);
        for (int i = 1; i <= testSize; i++) {
            ids.add(i);
        }
        return ids;
    }

    private List<Integer> shuffledIds() {
        List<Integer> ids = orderedIds();
        Collections.shuffle(ids);
        return ids;
    }

    private void load(Conveyor<Integer, SmartLabel<TrioBuilder>, Trio> conveyor,
                      List<Integer> text1Ids,
                      List<Integer> text2Ids,
                      List<Integer> numberIds) {
        AtomicReference<CompletableFuture<Boolean>> f1Ref = new AtomicReference<>();
        AtomicReference<CompletableFuture<Boolean>> f2Ref = new AtomicReference<>();
        AtomicReference<CompletableFuture<Boolean>> f3Ref = new AtomicReference<>();

        CompletableFuture<Void> t1 = pool.runAsync(() -> text1Ids.forEach(key -> {
            f1Ref.set(conveyor.part().id(key).label(TrioPart.TEXT1).value("txt1_" + key).place());
            sleep(key, sleepTime);
        }));
        CompletableFuture<Void> t2 = pool.runAsync(() -> text2Ids.forEach(key -> {
            f2Ref.set(conveyor.part().id(key).label(TrioPart.TEXT2).value("txt2_" + key).place());
            sleep(key, sleepTime);
        }));
        CompletableFuture<Void> t3 = pool.runAsync(() -> numberIds.forEach(key -> {
            f3Ref.set(conveyor.part().id(key).label(TrioPart.NUMBER).value(key).place());
            sleep(key, sleepTime);
        }));

        CompletableFuture.allOf(t1, t2, t3).join();
        f1Ref.get().join();
        f2Ref.get().join();
        f3Ref.get().join();
    }

    private void sleep(int iteration, double frac) {
        if (iteration % sleepNumber != 0) {
            return;
        }
        int msec = (int) frac;
        double nsec = frac - msec;
        try {
            Thread.sleep(msec, (int) (999_999.0 * nsec));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Redis perf pacing", e);
        }
    }
}
