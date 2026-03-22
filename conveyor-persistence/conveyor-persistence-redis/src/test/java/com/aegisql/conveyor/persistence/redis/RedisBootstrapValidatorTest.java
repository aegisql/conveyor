package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisBootstrapValidatorTest extends RedisTestSupport {

    @Test
    void parsesAndValidatesRedisServerVersionStrings() {
        RedisBootstrapValidator.RedisServerVersion simple =
                RedisBootstrapValidator.parseServerVersion("# Server\nredis_version:7.4.0\n");
        RedisBootstrapValidator.RedisServerVersion withSuffix =
                RedisBootstrapValidator.parseServerVersion("# Server\nredis_version:7.4.1-beta2\n");

        assertEquals(new RedisBootstrapValidator.RedisServerVersion(7, 4, 0), simple);
        assertEquals(new RedisBootstrapValidator.RedisServerVersion(7, 4, 1), withSuffix);
        assertEquals(simple, RedisBootstrapValidator.requireSupportedServerVersion(simple));

        PersistenceException belowMinimum = assertThrows(
                PersistenceException.class,
                () -> RedisBootstrapValidator.requireSupportedServerVersion(new RedisBootstrapValidator.RedisServerVersion(2, 7, 9))
        );
        assertTrue(belowMinimum.getMessage().contains("minimum supported version"));
    }

    @Test
    void rejectsMissingOrMalformedRedisVersionInfo() {
        PersistenceException missing = assertThrows(
                PersistenceException.class,
                () -> RedisBootstrapValidator.parseServerVersion("# Server\nrole:master\n")
        );
        assertTrue(missing.getMessage().contains("redis_version"));

        PersistenceException malformed = assertThrows(
                PersistenceException.class,
                () -> RedisBootstrapValidator.parseServerVersion("# Server\nredis_version:not-a-version\n")
        );
        assertTrue(malformed.getMessage().contains("Unable to parse Redis server version"));
    }

    @Test
    void validatesRequiredFeatureProbesAndAlwaysCleansUpProbeKeys() {
        RecordingFeatureAccess access = new RecordingFeatureAccess();
        RedisBootstrapValidator.validateRequiredFeatures(access, "conv:{validator-success}");

        assertEquals(List.of("sequence", "string", "hash", "set", "sorted-set", "lua"), access.calls);
        assertEquals(1, access.cleanupCalls);
        assertEquals(6, access.cleanedKeys.size());
        assertTrue(access.cleanedKeys.stream().allMatch(key -> key.startsWith("conv:{validator-success}:bootstrap:probe:")));
    }

    @Test
    void wrapsFeatureProbeFailuresAndStillCleansUpProbeKeys() {
        RecordingFeatureAccess access = new RecordingFeatureAccess();
        access.failOn = "lua";

        PersistenceException error = assertThrows(
                PersistenceException.class,
                () -> RedisBootstrapValidator.validateRequiredFeatures(access, "conv:{validator-failure}")
        );

        assertTrue(error.getMessage().contains("required feature validation failed"));
        assertEquals(List.of("sequence", "string", "hash", "set", "sorted-set", "lua"), access.calls);
        assertEquals(1, access.cleanupCalls);
        assertEquals(6, access.cleanedKeys.size());
    }

    @Test
    void validatesLiveRedisServerAndRequiredFeatures() {
        try (JedisPooled jedis = openRedis()) {
            RedisBootstrapValidator.RedisServerVersion version =
                    RedisBootstrapValidator.validate(jedis, "conv:{validator-live}");

            assertNotNull(version);
            assertTrue(version.compareTo(RedisBootstrapValidator.MIN_SUPPORTED_VERSION) >= 0);
        }
    }

    private static final class RecordingFeatureAccess implements RedisBootstrapValidator.FeatureAccess {
        private final ArrayList<String> calls = new ArrayList<>();
        private final ArrayList<String> cleanedKeys = new ArrayList<>();
        private String failOn;
        private int cleanupCalls;

        @Override
        public void validateSequenceProbe(String key) {
            record("sequence");
        }

        @Override
        public void validateStringProbe(String key) {
            record("string");
        }

        @Override
        public void validateHashProbe(String key) {
            record("hash");
        }

        @Override
        public void validateSetProbe(String key) {
            record("set");
        }

        @Override
        public void validateSortedSetProbe(String key) {
            record("sorted-set");
        }

        @Override
        public void validateLuaScriptingProbe(String key) {
            record("lua");
        }

        @Override
        public void cleanupProbeKeys(Collection<String> keys) {
            cleanupCalls++;
            cleanedKeys.addAll(keys);
        }

        private void record(String step) {
            calls.add(step);
            if (step.equals(failOn)) {
                throw new IllegalStateException("boom at " + step);
            }
        }
    }
}
