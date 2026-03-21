package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void createsExplicitPoolAndClientConfigFromConnectionSettings() {
        RedisConnectionSettings settings = new RedisConnectionSettings(
                "redis://localhost:6379/2",
                12,
                7,
                3,
                1_100,
                2_200,
                3_300,
                5,
                "builder-client",
                "builder-user",
                "builder-pass",
                true
        );

        ConnectionPoolConfig poolConfig = RedisConnectionFactory.createPoolConfig(settings);
        DefaultJedisClientConfig clientConfig = RedisConnectionFactory.createClientConfig(settings);
        HostAndPort endpoint = RedisConnectionFactory.hostAndPort(java.net.URI.create(settings.redisUri()));

        assertEquals(12, poolConfig.getMaxTotal());
        assertEquals(7, poolConfig.getMaxIdle());
        assertEquals(3, poolConfig.getMinIdle());

        assertEquals(1_100, clientConfig.getConnectionTimeoutMillis());
        assertEquals(2_200, clientConfig.getSocketTimeoutMillis());
        assertEquals(3_300, clientConfig.getBlockingSocketTimeoutMillis());
        assertEquals(5, clientConfig.getDatabase());
        assertEquals("builder-client", clientConfig.getClientName());
        assertEquals("builder-user", clientConfig.getUser());
        assertEquals("builder-pass", clientConfig.getPassword());
        assertTrue(clientConfig.isSsl());

        assertEquals("localhost", endpoint.getHost());
        assertEquals(6379, endpoint.getPort());
    }

    @Test
    void opensConnectionsFromSettingsWithLivePoolTuning() {
        RedisConnectionSettings settings = new RedisConnectionSettings(
                RedisConnectionFactory.DEFAULT_REDIS_URI,
                4,
                2,
                1,
                2_000,
                2_500,
                3_000,
                0,
                "redis-factory-test",
                null,
                null,
                false
        );

        try (JedisPooled jedis = RedisConnectionFactory.open(settings)) {
            assertEquals("PONG", jedis.ping());
            assertEquals(4, jedis.getPool().getMaxTotal());
            assertEquals(2, jedis.getPool().getMaxIdle());
            assertEquals(1, jedis.getPool().getMinIdle());
        }
    }
}
