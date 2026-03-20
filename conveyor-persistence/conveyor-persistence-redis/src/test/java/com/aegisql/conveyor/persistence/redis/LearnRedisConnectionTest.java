package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearnRedisConnectionTest extends RedisTestSupport {

    @Test
    void opensConnectionAndRespondsToPing() {
        try (JedisPooled jedis = openRedis()) {
            assertEquals("PONG", jedis.ping());
        }
    }
}
