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

    private static final String DELETE_PART_HELPER = """
            local function delete_parts(trackerKey, activeIdsKey, staticIdsKey, expiringIdsKey, partPrefix, partIdsPrefix, idMembers)
              local deletedCount = 0
              for _, idMember in ipairs(idMembers) do
                local reverseIndexKey = partPrefix .. idMember .. ':keys'
                local encodedKeys = redis.call('SMEMBERS', reverseIndexKey)
                for _, encodedKey in ipairs(encodedKeys) do
                  local partIdsKey = partIdsPrefix .. encodedKey
                  redis.call('ZREM', partIdsKey, idMember)
                  if redis.call('ZCARD', partIdsKey) == 0 then
                    redis.call('DEL', partIdsKey)
                    redis.call('SREM', trackerKey, partIdsKey)
                  end
                end

                redis.call('DEL', reverseIndexKey)
                redis.call('SREM', trackerKey, reverseIndexKey)
                redis.call('ZREM', activeIdsKey, idMember)
                redis.call('ZREM', staticIdsKey, idMember)
                redis.call('ZREM', expiringIdsKey, idMember)

                local payloadKey = partPrefix .. idMember .. ':payload'
                local metaKey = partPrefix .. idMember .. ':meta'
                redis.call('DEL', payloadKey, metaKey)
                redis.call('SREM', trackerKey, payloadKey, metaKey)
                deletedCount = deletedCount + 1
              end
              return deletedCount
            end
            """;

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

    private static final String DELETE_PARTS_SCRIPT = DELETE_PART_HELPER + """
            local trackerKey = KEYS[1]
            local activeIdsKey = KEYS[2]
            local staticIdsKey = KEYS[3]
            local expiringIdsKey = KEYS[4]

            local partPrefix = ARGV[1]
            local partIdsPrefix = ARGV[2]
            local idMembers = {}
            for i = 3, #ARGV do
              table.insert(idMembers, ARGV[i])
            end

            return delete_parts(trackerKey, activeIdsKey, staticIdsKey, expiringIdsKey, partPrefix, partIdsPrefix, idMembers)
            """;

    private static final String DELETE_KEYS_SCRIPT = DELETE_PART_HELPER + """
            local trackerKey = KEYS[1]
            local activeIdsKey = KEYS[2]
            local staticIdsKey = KEYS[3]
            local expiringIdsKey = KEYS[4]

            local partPrefix = ARGV[1]
            local partIdsPrefix = ARGV[2]
            local seen = {}
            local idMembers = {}

            for i = 3, #ARGV do
              local encodedKey = ARGV[i]
              local partIdsKey = partIdsPrefix .. encodedKey
              local idsForKey = redis.call('ZRANGE', partIdsKey, 0, -1)
              for _, idMember in ipairs(idsForKey) do
                if not seen[idMember] then
                  seen[idMember] = true
                  table.insert(idMembers, idMember)
                end
              end
            end

            return delete_parts(trackerKey, activeIdsKey, staticIdsKey, expiringIdsKey, partPrefix, partIdsPrefix, idMembers)
            """;

    private static final String DELETE_COMPLETED_KEYS_SCRIPT = """
            if #ARGV == 0 then
              return 0
            end
            return redis.call('SREM', KEYS[1], unpack(ARGV))
            """;

    private static final String DELETE_EXPIRED_PARTS_SCRIPT = DELETE_PART_HELPER + """
            local trackerKey = KEYS[1]
            local activeIdsKey = KEYS[2]
            local staticIdsKey = KEYS[3]
            local expiringIdsKey = KEYS[4]

            local partPrefix = ARGV[1]
            local partIdsPrefix = ARGV[2]
            local now = tonumber(ARGV[3])
            local idMembers = redis.call('ZRANGEBYSCORE', expiringIdsKey, '-inf', now)

            return delete_parts(trackerKey, activeIdsKey, staticIdsKey, expiringIdsKey, partPrefix, partIdsPrefix, idMembers)
            """;

    private static final String DELETE_ALL_SCRIPT = """
            local trackerKey = KEYS[1]
            local trackedKeys = redis.call('SMEMBERS', trackerKey)
            if #trackedKeys > 0 then
              redis.call('DEL', unpack(trackedKeys))
            end
            redis.call('DEL', trackerKey)
            return #trackedKeys
            """;

    private final JedisPooled jedis;
    private volatile String savePartSha;
    private volatile String deletePartsSha;
    private volatile String deleteKeysSha;
    private volatile String deleteCompletedKeysSha;
    private volatile String deleteExpiredPartsSha;
    private volatile String deleteAllSha;

    RedisLuaScriptBundle(JedisPooled jedis) {
        this.jedis = Objects.requireNonNull(jedis, "jedis must not be null");
    }

    void ensureLoaded() {
        if (savePartSha == null || deletePartsSha == null || deleteKeysSha == null
                || deleteCompletedKeysSha == null || deleteExpiredPartsSha == null || deleteAllSha == null) {
            synchronized (this) {
                loadAllScripts();
            }
        }
    }

    String savePartSha() {
        ensureLoaded();
        return savePartSha;
    }

    String deletePartsSha() {
        ensureLoaded();
        return deletePartsSha;
    }

    String deleteKeysSha() {
        ensureLoaded();
        return deleteKeysSha;
    }

    String deleteCompletedKeysSha() {
        ensureLoaded();
        return deleteCompletedKeysSha;
    }

    String deleteExpiredPartsSha() {
        ensureLoaded();
        return deleteExpiredPartsSha;
    }

    String deleteAllSha() {
        ensureLoaded();
        return deleteAllSha;
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
                loadAllScripts();
            }
            jedis.evalsha(savePartSha, keys, args);
        } catch (RuntimeException e) {
            throw new PersistenceException("Redis Lua savePart execution failed", e);
        }
    }

    void deleteParts(List<String> keys, List<String> args) {
        evalDeleteScript("deleteParts", () -> deletePartsSha, DELETE_PARTS_SCRIPT, keys, args);
    }

    void deleteKeys(List<String> keys, List<String> args) {
        evalDeleteScript("deleteKeys", () -> deleteKeysSha, DELETE_KEYS_SCRIPT, keys, args);
    }

    void deleteCompletedKeys(List<String> keys, List<String> args) {
        evalDeleteScript("deleteCompletedKeys", () -> deleteCompletedKeysSha, DELETE_COMPLETED_KEYS_SCRIPT, keys, args);
    }

    void deleteExpiredParts(List<String> keys, List<String> args) {
        evalDeleteScript("deleteExpiredParts", () -> deleteExpiredPartsSha, DELETE_EXPIRED_PARTS_SCRIPT, keys, args);
    }

    void deleteAll(List<String> keys) {
        evalDeleteScript("deleteAll", () -> deleteAllSha, DELETE_ALL_SCRIPT, keys, List.of());
    }

    private void evalDeleteScript(String operation, java.util.function.Supplier<String> shaSupplier, String script,
                                  List<String> keys, List<String> args) {
        Objects.requireNonNull(keys, "keys must not be null");
        Objects.requireNonNull(args, "args must not be null");
        ensureLoaded();
        try {
            jedis.evalsha(shaSupplier.get(), keys, args);
        } catch (JedisNoScriptException missing) {
            LOG.debug("Redis Lua {} script cache miss, reloading bundle", operation);
            synchronized (this) {
                loadAllScripts();
            }
            jedis.evalsha(shaForScript(script), keys, args);
        } catch (RuntimeException e) {
            throw new PersistenceException("Redis Lua " + operation + " execution failed", e);
        }
    }

    private void loadAllScripts() {
        savePartSha = jedis.scriptLoad(SAVE_PART_SCRIPT);
        deletePartsSha = jedis.scriptLoad(DELETE_PARTS_SCRIPT);
        deleteKeysSha = jedis.scriptLoad(DELETE_KEYS_SCRIPT);
        deleteCompletedKeysSha = jedis.scriptLoad(DELETE_COMPLETED_KEYS_SCRIPT);
        deleteExpiredPartsSha = jedis.scriptLoad(DELETE_EXPIRED_PARTS_SCRIPT);
        deleteAllSha = jedis.scriptLoad(DELETE_ALL_SCRIPT);
        LOG.debug("Loaded Redis Lua script bundle savePartSha={} deletePartsSha={} deleteKeysSha={} deleteCompletedKeysSha={} deleteExpiredPartsSha={} deleteAllSha={}",
                savePartSha, deletePartsSha, deleteKeysSha, deleteCompletedKeysSha, deleteExpiredPartsSha, deleteAllSha);
    }

    private String shaForScript(String script) {
        if (SAVE_PART_SCRIPT.equals(script)) {
            return savePartSha;
        }
        if (DELETE_PARTS_SCRIPT.equals(script)) {
            return deletePartsSha;
        }
        if (DELETE_KEYS_SCRIPT.equals(script)) {
            return deleteKeysSha;
        }
        if (DELETE_COMPLETED_KEYS_SCRIPT.equals(script)) {
            return deleteCompletedKeysSha;
        }
        if (DELETE_EXPIRED_PARTS_SCRIPT.equals(script)) {
            return deleteExpiredPartsSha;
        }
        if (DELETE_ALL_SCRIPT.equals(script)) {
            return deleteAllSha;
        }
        throw new IllegalArgumentException("Unknown Redis Lua script reference");
    }
}
