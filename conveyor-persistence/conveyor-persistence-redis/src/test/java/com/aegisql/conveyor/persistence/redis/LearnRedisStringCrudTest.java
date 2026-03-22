package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Learn-by-example test for Redis string values.
 *
 * <p>The point here is to demonstrate the simplest Redis record lifecycle using
 * one key: create it, read it back, overwrite it, and delete it.</p>
 */
class LearnRedisStringCrudTest extends RedisTestSupport {

    /**
     * Proves that a Redis string key can be created, read, updated in place, and
     * removed, leaving the key absent afterward.
     */
    @Test
    void demonstratesStringCreateReadUpdateDelete() {
        String key = testKey("string");

        try (JedisPooled jedis = openRedis()) {
            jedis.del(key);

            assertEquals("OK", jedis.set(key, "draft"));
            assertEquals("draft", jedis.get(key));

            assertEquals("OK", jedis.set(key, "final"));
            assertEquals("final", jedis.get(key));

            assertEquals(1L, jedis.del(key));
            assertFalse(jedis.exists(key));
        }
    }
}
