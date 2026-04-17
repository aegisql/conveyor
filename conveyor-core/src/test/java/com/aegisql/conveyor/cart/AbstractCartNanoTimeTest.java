package com.aegisql.conveyor.cart;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractCartNanoTimeTest {

    @Test
    void cartCreationNanoTimeShouldIncreaseForSequentialCreations() {
        long now = System.currentTimeMillis();

        Cart<Integer, String, String> c1 = new ShoppingCart<>(1, "v1", "l1", now, 0, 0);
        Cart<Integer, String, String> c2 = new ShoppingCart<>(2, "v2", "l2", now, 0, 0);
        Cart<Integer, String, String> c3 = new ShoppingCart<>(3, "v3", "l3", now, 0, 0);

        assertTrue(c1.getCartCreationNanoTime() < c2.getCartCreationNanoTime());
        assertTrue(c2.getCartCreationNanoTime() < c3.getCartCreationNanoTime());
    }

    @Test
    void cartCreationNanoTimeShouldRemainUniqueAcrossThreads() throws Exception {
        int cartCount = 256;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        var timestamps = ConcurrentHashMap.<Long>newKeySet();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < cartCount; i++) {
            int key = i;
            tasks.add(() -> {
                start.await();
                Cart<Integer, String, String> cart = new ShoppingCart<>(key, "v" + key, "l" + key);
                timestamps.add(cart.getCartCreationNanoTime());
                return null;
            });
        }

        try {
            var futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (var future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(cartCount, timestamps.size());
    }
}
