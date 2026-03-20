package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisConnectionFactoryTest extends RedisTestSupport {

    @Test
    void resolvesRedisUriAcrossPropertyEnvironmentAndDefaultBranches() {
        assertEquals("redis://property:6379",
                RedisConnectionFactory.resolveRedisUri(" redis://property:6379 ", "redis://env:6379"));
        assertEquals("redis://env:6379",
                RedisConnectionFactory.resolveRedisUri("   ", " redis://env:6379 "));
        assertEquals(RedisConnectionFactory.DEFAULT_REDIS_URI,
                RedisConnectionFactory.resolveRedisUri(null, "  "));
    }

    @Test
    void opensConnectionsDirectlyAndViaDefaultResolution() {
        try (JedisPooled direct = RedisConnectionFactory.open(RedisConnectionFactory.DEFAULT_REDIS_URI)) {
            assertEquals("PONG", direct.ping());
        }

        String originalProperty = System.getProperty(RedisConnectionFactory.REDIS_URI_PROPERTY);
        try {
            System.setProperty(RedisConnectionFactory.REDIS_URI_PROPERTY, RedisConnectionFactory.DEFAULT_REDIS_URI);
            try (JedisPooled fromDefault = RedisConnectionFactory.openDefault()) {
                assertEquals("PONG", fromDefault.ping());
            }
        } finally {
            if (originalProperty == null) {
                System.clearProperty(RedisConnectionFactory.REDIS_URI_PROPERTY);
            } else {
                System.setProperty(RedisConnectionFactory.REDIS_URI_PROPERTY, originalProperty);
            }
        }
    }
}
