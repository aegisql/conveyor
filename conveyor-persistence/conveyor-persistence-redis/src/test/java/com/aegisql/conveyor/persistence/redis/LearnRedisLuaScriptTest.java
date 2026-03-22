package com.aegisql.conveyor.persistence.redis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Learn-by-example tests for Redis Lua scripting.
 *
 * <p>These tests are intentionally small. They show the two scripting flows that
 * matter for this module:
 * direct {@code EVAL} execution and cached {@code SCRIPT LOAD}/{@code EVALSHA}
 * execution.</p>
 */
class LearnRedisLuaScriptTest extends RedisTestSupport {

    /**
     * Evaluates a tiny Lua script that reads an existing Redis key and returns
     * the stored value unchanged.
     */
    @Test
    void evaluatesSimpleLuaScriptAgainstRedisData() {
        String key = testKey("lua-eval");

        try (JedisPooled jedis = openRedis()) {
            jedis.set(key, "value-from-redis");

            Object result = jedis.eval("return redis.call('GET', KEYS[1])", List.of(key), List.of());

            assertEquals("value-from-redis", result);
            jedis.del(key);
        }
    }

    /**
     * Loads a Lua script into Redis, executes it by SHA, and verifies that the
     * script can both mutate Redis state and return structured data.
     */
    @Test
    void loadsLuaScriptAndExecutesItBySha() {
        String key = testKey("lua-evalsha");

        try (JedisPooled jedis = openRedis()) {
            String sha = jedis.scriptLoad("return {redis.call('INCR', KEYS[1]),ARGV[1]}");

            Object result = jedis.evalsha(sha, List.of(key), List.of("side-arg"));

            assertInstanceOf(List.class, result);
            assertEquals(List.of(1L, "side-arg"), result);
            jedis.del(key);
        }
    }
}
