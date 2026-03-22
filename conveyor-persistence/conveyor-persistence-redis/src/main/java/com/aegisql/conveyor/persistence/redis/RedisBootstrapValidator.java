package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RedisBootstrapValidator {

    private static final Logger LOG = LoggerFactory.getLogger(RedisBootstrapValidator.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");
    static final RedisServerVersion MIN_SUPPORTED_VERSION = new RedisServerVersion(2, 8, 0);

    private RedisBootstrapValidator() {
    }

    static RedisServerVersion validate(JedisPooled jedis, String namespace) {
        Objects.requireNonNull(jedis, "jedis must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");

        RedisServerVersion version = requireSupportedServerVersion(parseServerVersion(jedis.info("server")));
        validateRequiredFeatures(new JedisFeatureAccess(jedis), namespace);
        LOG.debug("Validated Redis bootstrap compatibility namespace={} version={}", namespace, version);
        return version;
    }

    static RedisServerVersion parseServerVersion(String serverInfo) {
        String versionValue = parseInfoValue(serverInfo, "redis_version");
        Matcher matcher = VERSION_PATTERN.matcher(versionValue);
        if (!matcher.matches()) {
            throw new PersistenceException("Unable to parse Redis server version: " + versionValue);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new RedisServerVersion(major, minor, patch);
    }

    static RedisServerVersion requireSupportedServerVersion(RedisServerVersion version) {
        Objects.requireNonNull(version, "version must not be null");
        if (version.compareTo(MIN_SUPPORTED_VERSION) < 0) {
            throw new PersistenceException("Redis server version " + version
                    + " is below the minimum supported version " + MIN_SUPPORTED_VERSION);
        }
        return version;
    }

    static void validateRequiredFeatures(FeatureAccess access, String namespace) {
        Objects.requireNonNull(access, "access must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");

        String probePrefix = namespace + ":bootstrap:probe:" + UUID.randomUUID();
        ArrayList<String> probeKeys = new ArrayList<>();
        try {
            probeKeys.add(probePrefix + ":seq");
            access.validateSequenceProbe(probeKeys.getLast());

            probeKeys.add(probePrefix + ":string");
            access.validateStringProbe(probeKeys.getLast());

            probeKeys.add(probePrefix + ":hash");
            access.validateHashProbe(probeKeys.getLast());

            probeKeys.add(probePrefix + ":set");
            access.validateSetProbe(probeKeys.getLast());

            probeKeys.add(probePrefix + ":zset");
            access.validateSortedSetProbe(probeKeys.getLast());

            probeKeys.add(probePrefix + ":lua");
            access.validateLuaScriptingProbe(probeKeys.getLast());
        } catch (RuntimeException e) {
            throw new PersistenceException("Redis required feature validation failed for namespace " + namespace, e);
        } finally {
            access.cleanupProbeKeys(probeKeys);
        }
    }

    private static String parseInfoValue(String info, String key) {
        if (info == null || info.isBlank()) {
            throw new PersistenceException("Redis INFO server output is empty");
        }
        String prefix = key + ":";
        for (String line : info.split("\\R")) {
            if (line.startsWith(prefix)) {
                String value = line.substring(prefix.length()).trim();
                if (!value.isEmpty()) {
                    return value;
                }
                break;
            }
        }
        throw new PersistenceException("Redis INFO server output does not contain " + key);
    }

    interface FeatureAccess {
        void validateSequenceProbe(String key);

        void validateStringProbe(String key);

        void validateHashProbe(String key);

        void validateSetProbe(String key);

        void validateSortedSetProbe(String key);

        void validateLuaScriptingProbe(String key);

        void cleanupProbeKeys(Collection<String> keys);
    }

    private static final class JedisFeatureAccess implements FeatureAccess {
        private final JedisPooled jedis;

        private JedisFeatureAccess(JedisPooled jedis) {
            this.jedis = jedis;
        }

        @Override
        public void validateSequenceProbe(String key) {
            long first = jedis.incr(key);
            long second = jedis.incr(key);
            if (first != 1L || second != 2L) {
                throw new PersistenceException("Redis INCR probe produced unexpected values for " + key + ": " + first + ", " + second);
            }
        }

        @Override
        public void validateStringProbe(String key) {
            jedis.set(key, "value");
            String value = jedis.get(key);
            if (!"value".equals(value)) {
                throw new PersistenceException("Redis string probe returned unexpected value for " + key + ": " + value);
            }
        }

        @Override
        public void validateHashProbe(String key) {
            jedis.hset(key, Map.of("fieldA", "A", "fieldB", "B"));
            Map<String, String> values = jedis.hgetAll(key);
            if (!"A".equals(values.get("fieldA")) || !"B".equals(values.get("fieldB"))) {
                throw new PersistenceException("Redis hash probe returned unexpected values for " + key + ": " + values);
            }
        }

        @Override
        public void validateSetProbe(String key) {
            jedis.sadd(key, "left", "right");
            Set<String> values = jedis.smembers(key);
            if (!values.contains("left") || !values.contains("right")) {
                throw new PersistenceException("Redis set probe returned unexpected values for " + key + ": " + values);
            }
            jedis.srem(key, "left", "right");
        }

        @Override
        public void validateSortedSetProbe(String key) {
            jedis.zadd(key, 1.0d, "one");
            jedis.zadd(key, 2.0d, "two");
            List<String> ordered = jedis.zrange(key, 0, -1);
            List<String> byScore = jedis.zrangeByScore(key, 1.0d, 1.0d);
            long size = jedis.zcard(key);
            jedis.zrem(key, "one", "two");
            if (!List.of("one", "two").equals(ordered) || !List.of("one").equals(byScore) || size != 2L) {
                throw new PersistenceException("Redis sorted-set probe returned unexpected values for " + key
                        + ": ordered=" + ordered + ", byScore=" + byScore + ", size=" + size);
            }
        }

        @Override
        public void validateLuaScriptingProbe(String key) {
            String sha = jedis.scriptLoad("return {KEYS[1],ARGV[1]}");
            Object result = jedis.evalsha(sha, List.of(key), List.of("value"));
            if (!(result instanceof List<?> values)
                    || values.size() != 2
                    || !key.equals(String.valueOf(values.getFirst()))
                    || !"value".equals(String.valueOf(values.get(1)))) {
                throw new PersistenceException("Redis Lua probe returned unexpected values for " + key + ": " + result);
            }
        }

        @Override
        public void cleanupProbeKeys(Collection<String> keys) {
            if (keys == null || keys.isEmpty()) {
                return;
            }
            jedis.del(keys.toArray(String[]::new));
        }
    }

    record RedisServerVersion(int major, int minor, int patch) implements Comparable<RedisServerVersion> {
        @Override
        public int compareTo(RedisServerVersion other) {
            int majorCompare = Integer.compare(major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            return Integer.compare(patch, other.patch);
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}
