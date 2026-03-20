package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LearnRedisStringCrudTest extends RedisTestSupport {

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
