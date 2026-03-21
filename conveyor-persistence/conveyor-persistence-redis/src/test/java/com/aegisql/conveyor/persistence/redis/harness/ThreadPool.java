package com.aegisql.conveyor.persistence.redis.harness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    private final ExecutorService pool;

    public ThreadPool(int n) {
        this.pool = Executors.newFixedThreadPool(n);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, pool);
    }

    public void shutdown() {
        pool.shutdown();
    }
}
