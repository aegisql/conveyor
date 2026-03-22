package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.util.List;
import java.util.Objects;

final class RedisLuaScriptBundle {

    private static final Logger LOG = LoggerFactory.getLogger(RedisLuaScriptBundle.class);

    static final String SCRIPT_MODE = "lua";
    static final String BUNDLE_VERSION = "1";

    private static final String SAVE_PART_SCRIPT = """
            local trackerKey = KEYS[1]
            for i = 2, #KEYS do
              redis.call('SADD', trackerKey, KEYS[i])
            end

            local metaKey = KEYS[2]
            local payloadKey = KEYS[3]
            local reverseIndexKey = KEYS[4]
            local activeIdsKey = KEYS[5]
            local staticIdsKey = KEYS[6]
            local expiringIdsKey = KEYS[7]
            local partIdsKey = KEYS[8]

            local cursor = 1
            local idMember = ARGV[cursor]
            cursor = cursor + 1
            local idScore = tonumber(idMember)
            local expirationTime = tonumber(ARGV[cursor])
            cursor = cursor + 1
            local isStaticPart = ARGV[cursor] == '1'
            cursor = cursor + 1
            local hasPayload = ARGV[cursor] == '1'
            cursor = cursor + 1
            local payloadValue = ARGV[cursor]
            cursor = cursor + 1
            local hasIndexedKey = ARGV[cursor] == '1'
            cursor = cursor + 1
            local encodedKey = ARGV[cursor]
            cursor = cursor + 1
            local metaPairsCount = tonumber(ARGV[cursor])
            cursor = cursor + 1

            local metaPairs = {}
            for i = 1, metaPairsCount * 2 do
              metaPairs[i] = ARGV[cursor]
              cursor = cursor + 1
            end

            redis.call('DEL', metaKey)
            if metaPairsCount > 0 then
              redis.call('HSET', metaKey, unpack(metaPairs))
            end

            if hasPayload then
              redis.call('SET', payloadKey, payloadValue)
            else
              redis.call('DEL', payloadKey)
            end

            if isStaticPart then
              redis.call('ZADD', staticIdsKey, idScore, idMember)
              redis.call('ZREM', activeIdsKey, idMember)
            else
              redis.call('ZADD', activeIdsKey, idScore, idMember)
              redis.call('ZREM', staticIdsKey, idMember)
            end

            if expirationTime > 0 then
              redis.call('ZADD', expiringIdsKey, expirationTime, idMember)
            else
              redis.call('ZREM', expiringIdsKey, idMember)
            end

            if hasIndexedKey then
              redis.call('ZADD', partIdsKey, idScore, idMember)
              redis.call('SADD', reverseIndexKey, encodedKey)
            end

            return idMember
            """;

    private final JedisPooled jedis;
    private volatile String savePartSha;

    RedisLuaScriptBundle(JedisPooled jedis) {
        this.jedis = Objects.requireNonNull(jedis, "jedis must not be null");
    }

    void ensureLoaded() {
        if (savePartSha == null) {
            synchronized (this) {
                if (savePartSha == null) {
                    savePartSha = jedis.scriptLoad(SAVE_PART_SCRIPT);
                    LOG.debug("Loaded Redis Lua savePart bundle sha={}", savePartSha);
                }
            }
        }
    }

    String savePartSha() {
        ensureLoaded();
        return savePartSha;
    }

    void savePart(List<String> keys, List<String> args) {
        Objects.requireNonNull(keys, "keys must not be null");
        Objects.requireNonNull(args, "args must not be null");
        ensureLoaded();
        try {
            jedis.evalsha(savePartSha, keys, args);
        } catch (JedisNoScriptException missing) {
            LOG.debug("Redis Lua savePart script cache miss, reloading bundle");
            synchronized (this) {
                savePartSha = jedis.scriptLoad(SAVE_PART_SCRIPT);
            }
            jedis.evalsha(savePartSha, keys, args);
        } catch (RuntimeException e) {
            throw new PersistenceException("Redis Lua savePart execution failed", e);
        }
    }
}
