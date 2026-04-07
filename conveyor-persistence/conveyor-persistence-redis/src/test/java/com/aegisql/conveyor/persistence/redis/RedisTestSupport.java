package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

abstract class RedisTestSupport {

    private static final ThreadLocal<TestScope> CURRENT_SCOPE = new ThreadLocal<>();

    @BeforeAll
    static void requireRedisAvailability() {
        try (JedisPooled ignored = openRedis()) {
            // Availability check only. Tests open their own clients as needed.
        }
    }

    @BeforeEach
    void prepareTestScope(TestInfo testInfo) {
        String className = sanitize(testInfo.getTestClass().map(Class::getSimpleName).orElse("unknown-class"));
        String methodName = sanitize(testInfo.getTestMethod().map(method -> method.getName()).orElse("unknown-method"));
        CURRENT_SCOPE.set(new TestScope(className + "-" + methodName));
    }

    @AfterEach
    void clearTestScope() {
        CURRENT_SCOPE.remove();
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
        TestScope scope = currentScope();
        String key = "learn-redis:" + scope.testId + ":" + sanitize(suffix);
        if (scope.preparedKeys.add(key)) {
            try (JedisPooled jedis = openRedis()) {
                jedis.del(key);
            }
        }
        return key;
    }

    protected static String testNamespace(String suffix) {
        TestScope scope = currentScope();
        String namespace = "redis-persistence-" + scope.testId + "-" + sanitize(suffix);
        if (scope.preparedNamespaces.add(namespace)) {
            wipeNamespace(namespace);
        }
        return namespace;
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

    private static TestScope currentScope() {
        TestScope scope = CURRENT_SCOPE.get();
        if (scope == null) {
            throw new IllegalStateException("Redis test scope is not initialized");
        }
        return scope;
    }

    private static void wipeNamespace(String namespace) {
        String pattern = "conv:{" + namespace + "}*";
        try (JedisPooled jedis = openRedis()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(pattern).count(500);
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                List<String> keys = result.getResult();
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(String[]::new));
                }
                cursor = result.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        }
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        sanitized = sanitized.replaceAll("^-+", "").replaceAll("-+$", "");
        return sanitized.isBlank() ? "unnamed" : sanitized;
    }

    private static final class TestScope {
        private final String testId;
        private final Set<String> preparedNamespaces = new HashSet<>();
        private final Set<String> preparedKeys = new HashSet<>();

        private TestScope(String testId) {
            this.testId = testId;
        }
    }
}
