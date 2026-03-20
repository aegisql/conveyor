package com.aegisql.conveyor.persistence.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOG.debug("Opening Redis pooled connection for URI {}", redisUri);
        return new JedisPooled(URI.create(redisUri));
    }
}
