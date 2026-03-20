package com.aegisql.conveyor.persistence.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class RedisClientHandle implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RedisClientHandle.class);

    private final JedisPooled jedis;
    private final boolean closeUnderlying;
    private final AtomicInteger references = new AtomicInteger(1);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private RedisClientHandle(JedisPooled jedis, boolean closeUnderlying) {
        this.jedis = Objects.requireNonNull(jedis, "jedis must not be null");
        this.closeUnderlying = closeUnderlying;
    }

    static RedisClientHandle owned(JedisPooled jedis) {
        return new RedisClientHandle(jedis, true);
    }

    static RedisClientHandle external(JedisPooled jedis) {
        return new RedisClientHandle(jedis, false);
    }

    RedisClientHandle retain() {
        while (true) {
            int current = references.get();
            if (current <= 0) {
                throw new IllegalStateException("Redis client handle is already closed");
            }
            if (references.compareAndSet(current, current + 1)) {
                LOG.trace("Retained Redis client handle refs={} closeUnderlying={}", current + 1, closeUnderlying);
                return this;
            }
        }
    }

    JedisPooled jedis() {
        return jedis;
    }

    boolean closesUnderlying() {
        return closeUnderlying;
    }

    int referenceCount() {
        return Math.max(references.get(), 0);
    }

    @Override
    public void close() {
        int remaining = references.decrementAndGet();
        LOG.trace("Releasing Redis client handle refs={} closeUnderlying={}", remaining, closeUnderlying);
        if (remaining > 0) {
            return;
        }
        if (remaining < 0) {
            LOG.warn("Redis client handle released more times than retained");
            return;
        }
        if (closeUnderlying && closed.compareAndSet(false, true)) {
            LOG.debug("Closing owned Redis pooled client");
            jedis.close();
        }
    }
}
