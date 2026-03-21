package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.Persistence;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import redis.clients.jedis.JedisPooled;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

abstract class RedisTestSupport {

    @BeforeAll
    static void requireRedisAvailability() {
        try (JedisPooled ignored = openRedis()) {
            // Availability check only. Tests open their own clients as needed.
        }
    }

    protected static JedisPooled openRedis() {
        String redisUri = RedisConnectionFactory.resolveRedisUri();
        try {
            JedisPooled jedis = RedisConnectionFactory.open(redisUri);
            Assumptions.assumeTrue("PONG".equals(jedis.ping()), "Redis did not respond with PONG at " + redisUri);
            return jedis;
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Redis is not available at " + redisUri + ": " + e.getMessage());
            throw new IllegalStateException("Redis assumption should have aborted the test", e);
        }
    }

    protected static String testKey(String suffix) {
        return "learn-redis:" + suffix + ":" + UUID.randomUUID();
    }

    protected static String testNamespace(String suffix) {
        return "redis-persistence-" + suffix + "-" + UUID.randomUUID();
    }

    protected static Persistence<Integer> newPersistence(String suffix) {
        return new RedisPersistenceBuilder<Integer>(testNamespace(suffix)).build();
    }

    protected static Persistence<Integer> newPersistence(String suffix, RedisPersistenceBuilder<Integer> builder) {
        return builder.build();
    }

    protected static int getPerfTestSize() {
        return getEnvOrDefaultInteger("REDIS_PERF_TEST_SIZE", getEnvOrDefaultInteger("PERF_TEST_SIZE", 10_000));
    }

    protected static void waitUntilArchived(Persistence<Integer> persistence, int testSize) {
        long previousParts = -1L;
        int sameNumber = 0;
        while (true) {
            long parts = persistence.getNumberOfParts();
            if (parts <= 0) {
                return;
            }
            if (parts == previousParts) {
                sameNumber++;
                if (sameNumber > 50) {
                    throw new IllegalStateException("Stuck on Redis archiving with number of parts = " + parts);
                }
            } else {
                sameNumber = 0;
            }
            long percent = 100 - (100 * parts / Math.max(1, 3L * testSize));
            System.out.print("\r" + percent + "%");
            sleep(100, TimeUnit.MILLISECONDS);
            previousParts = parts;
        }
    }

    private static int getEnvOrDefaultInteger(String param, int defValue) {
        String value = System.getenv(param);
        if (value != null) {
            return Integer.parseInt(value);
        }
        value = System.getProperty(param);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return defValue;
    }

    private static void sleep(long value, TimeUnit unit) {
        try {
            unit.sleep(value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Redis archive completion", e);
        }
    }
}
