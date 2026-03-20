package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LearnRedisHashCrudTest extends RedisTestSupport {

    @Test
    void demonstratesHashCreateReadUpdateDelete() {
        String key = testKey("hash");

        try (JedisPooled jedis = openRedis()) {
            jedis.del(key);

            assertEquals(2L, jedis.hset(key, Map.of(
                    "user", "alice",
                    "balance", "10.50"
            )));

            assertEquals("alice", jedis.hget(key, "user"));
            assertEquals("10.50", jedis.hgetAll(key).get("balance"));

            assertEquals(0L, jedis.hset(key, "balance", "11.00"));
            assertEquals("11.00", jedis.hget(key, "balance"));

            assertEquals(1L, jedis.del(key));
            assertFalse(jedis.exists(key));
        }
    }
}
