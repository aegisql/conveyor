package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Small connectivity proof for the local Redis test environment.
 *
 * <p>This test is meant to show the minimum happy path we rely on before
 * running any persistence tests: the client opens successfully and Redis
 * answers a simple health-style command.</p>
 */
class LearnRedisConnectionTest extends RedisTestSupport {

    /**
     * Opens a Redis client and verifies that the server responds to {@code PING}
     * with the standard {@code PONG} reply.
     */
    @Test
    void opensConnectionAndRespondsToPing() {
        try (JedisPooled jedis = openRedis()) {
            assertEquals("PONG", jedis.ping());
        }
    }
}
