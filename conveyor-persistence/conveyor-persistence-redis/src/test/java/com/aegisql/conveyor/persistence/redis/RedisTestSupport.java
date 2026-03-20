package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.Persistence;
import org.junit.jupiter.api.Assumptions;
import redis.clients.jedis.JedisPooled;

import java.util.UUID;

abstract class RedisTestSupport {

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
}
