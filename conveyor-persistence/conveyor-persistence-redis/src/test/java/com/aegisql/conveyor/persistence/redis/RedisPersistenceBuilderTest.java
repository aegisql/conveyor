package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPersistenceBuilderTest extends RedisTestSupport {

    @Test
    void validatesBuilderArgumentsAndSupportsLazyInitialization() throws Exception {
        assertThrows(NullPointerException.class, () -> new RedisPersistenceBuilder<>(null));

        RedisPersistenceBuilder<Integer> builder = new RedisPersistenceBuilder<Integer>(testNamespace("builder-validation"));
        assertThrows(NullPointerException.class, () -> builder.redisUri(null));
        assertThrows(NullPointerException.class, () -> builder.nonPersistentProperty(null));
        assertThrows(NullPointerException.class, () -> builder.persistentPartFilter(null));

        try (Persistence<Integer> persistence = builder.autoInit(false).build()) {
            persistence.archiveAll();
            assertEquals(1L, persistence.nextUniquePartId());
        }
    }

    @Test
    void handlesNoOpBranchesAndNonPersistentFilter() throws Exception {
        try (Persistence<Integer> persistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-noops"))
                .persistentPartFilter(cart -> false)
                .build()) {
            persistence.archiveAll();

            ShoppingCart<Integer, String, String> cart = new ShoppingCart<>(1, "value", "label");
            assertFalse(persistence.isPartPersistent(cart));

            persistence.savePart(1L, cart);
            assertNull(persistence.getPart(1L));
            assertEquals(0L, persistence.getNumberOfParts());

            persistence.savePartId(null, 1L);
            persistence.saveCompletedBuildKey(null);
            assertTrue(persistence.getAllPartIds(null).isEmpty());

            assertDoesNotThrow(() -> persistence.archiveParts(null));
            assertDoesNotThrow(() -> persistence.archiveParts(java.util.List.of()));
            assertDoesNotThrow(() -> persistence.archiveKeys(null));
            assertDoesNotThrow(() -> persistence.archiveKeys(Arrays.asList(null, 1)));
            assertDoesNotThrow(() -> persistence.archiveCompleteKeys(null));
            assertDoesNotThrow(() -> persistence.archiveCompleteKeys(java.util.List.of()));
            assertTrue(persistence.toString().contains("RedisPersistence["));
        }
    }

    @Test
    void rejectsNonSerializableKeysThroughPublicApi() throws Exception {
        openRedis().close();

        try (Persistence<Object> persistence = new RedisPersistenceBuilder<Object>(testNamespace("builder-nonserializable")).build()) {
            PersistenceException completedKeyError =
                    assertThrows(PersistenceException.class, () -> persistence.saveCompletedBuildKey(new Object()));
            assertTrue(completedKeyError.getMessage().contains("Value is not serializable"));

            PersistenceException partIdError =
                    assertThrows(PersistenceException.class, () -> persistence.savePartId(new Object(), 1L));
            assertTrue(partIdError.getMessage().contains("Value is not serializable"));

            PersistenceException getIdsError =
                    assertThrows(PersistenceException.class, () -> persistence.getAllPartIds(new Object()));
            assertTrue(getIdsError.getMessage().contains("Value is not serializable"));

            PersistenceException archiveKeysError =
                    assertThrows(PersistenceException.class, () -> persistence.archiveKeys(List.of(new Object())));
            assertTrue(archiveKeysError.getMessage().contains("Value is not serializable"));
        }
    }

    @Test
    void rejectsNullInInternalRedisEncodingHelper() throws Exception {
        openRedis().close();

        try (Persistence<Integer> rawPersistence = new RedisPersistenceBuilder<Integer>(testNamespace("builder-null-encoding")).build()) {
            RedisPersistence<Integer> persistence = (RedisPersistence<Integer>) rawPersistence;
            Method encodeSerializable = RedisPersistence.class.getDeclaredMethod("encodeSerializable", Object.class);
            encodeSerializable.setAccessible(true);

            InvocationTargetException error = assertThrows(
                    InvocationTargetException.class,
                    () -> encodeSerializable.invoke(persistence, new Object[]{null})
            );

            Throwable cause = error.getCause();
            assertInstanceOf(PersistenceException.class, cause);
            assertEquals("Redis persistence cannot encode null values as indexed keys", cause.getMessage());
        }
    }
}
