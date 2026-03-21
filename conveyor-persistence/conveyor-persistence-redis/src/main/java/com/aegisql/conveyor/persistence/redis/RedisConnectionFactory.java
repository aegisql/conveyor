package com.aegisql.conveyor.persistence.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.net.URI;

public final class RedisConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RedisConnectionFactory.class);

    public static final String REDIS_URI_PROPERTY = "conveyor.persistence.redis.uri";
    public static final String REDIS_URI_ENV = "CONVEYOR_PERSISTENCE_REDIS_URI";
    public static final String DEFAULT_REDIS_URI = "redis://localhost:6379";

    private RedisConnectionFactory() {
    }

    public static String resolveRedisUri() {
        return resolveRedisUri(System.getProperty(REDIS_URI_PROPERTY), System.getenv(REDIS_URI_ENV));
    }

    static String resolveRedisUri(String propertyValue, String envValue) {
        if (propertyValue != null && !propertyValue.isBlank()) {
            String resolved = propertyValue.trim();
            LOG.debug("Using Redis URI from system property {}: {}", REDIS_URI_PROPERTY, resolved);
            return resolved;
        }
        if (envValue != null && !envValue.isBlank()) {
            String resolved = envValue.trim();
            LOG.debug("Using Redis URI from environment variable {}: {}", REDIS_URI_ENV, resolved);
            return resolved;
        }
        LOG.debug("Using default Redis URI: {}", DEFAULT_REDIS_URI);
        return DEFAULT_REDIS_URI;
    }

    public static JedisPooled openDefault() {
        return open(resolveRedisUri());
    }

    public static JedisPooled open(String redisUri) {
        return open(new RedisConnectionSettings(redisUri, null, null, null, null, null, null, null, null, null, null, null));
    }

    static JedisPooled open(RedisConnectionSettings settings) {
        URI redisUri = URI.create(settings.redisUri());
        HostAndPort endpoint = hostAndPort(redisUri);
        ConnectionPoolConfig poolConfig = createPoolConfig(settings);
        DefaultJedisClientConfig clientConfig = createClientConfig(settings);

        LOG.debug(
                "Opening Redis pooled connection uri={} host={} port={} poolTuned={} clientTuned={} db={} clientName={} ssl={}",
                settings.redisUri(),
                endpoint.getHost(),
                endpoint.getPort(),
                settings.hasCustomPoolConfig(),
                settings.hasCustomClientConfig(),
                clientConfig.getDatabase(),
                clientConfig.getClientName(),
                clientConfig.isSsl()
        );
        return new JedisPooled(poolConfig, endpoint, clientConfig);
    }

    static ConnectionPoolConfig createPoolConfig(RedisConnectionSettings settings) {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        if (settings.maxTotal() != null) {
            poolConfig.setMaxTotal(settings.maxTotal());
        }
        if (settings.maxIdle() != null) {
            poolConfig.setMaxIdle(settings.maxIdle());
        }
        if (settings.minIdle() != null) {
            poolConfig.setMinIdle(settings.minIdle());
        }
        return poolConfig;
    }

    static DefaultJedisClientConfig createClientConfig(RedisConnectionSettings settings) {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder(URI.create(settings.redisUri()));
        if (settings.connectionTimeoutMillis() != null) {
            builder.connectionTimeoutMillis(settings.connectionTimeoutMillis());
        }
        if (settings.socketTimeoutMillis() != null) {
            builder.socketTimeoutMillis(settings.socketTimeoutMillis());
        }
        if (settings.blockingSocketTimeoutMillis() != null) {
            builder.blockingSocketTimeoutMillis(settings.blockingSocketTimeoutMillis());
        }
        if (settings.database() != null) {
            builder.database(settings.database());
        }
        if (settings.clientName() != null) {
            builder.clientName(settings.clientName());
        }
        if (settings.user() != null) {
            builder.user(settings.user());
        }
        if (settings.password() != null) {
            builder.password(settings.password());
        }
        if (settings.ssl() != null) {
            builder.ssl(settings.ssl());
        }
        return builder.build();
    }

    static HostAndPort hostAndPort(URI redisUri) {
        String host = redisUri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Redis URI must include a host: " + redisUri);
        }
        int port = redisUri.getPort() > 0 ? redisUri.getPort() : 6379;
        return new HostAndPort(host, port);
    }
}
