package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.converters.EncryptingConverter;
import com.aegisql.conveyor.persistence.encryption.EncryptingConverterBuilder;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;
import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;

final class ProfileEncryptionSupport {

    private ProfileEncryptionSupport() {
    }

    static <L> ConverterAdviser<L> newDecryptingConverterAdviser(PersistenceProfile profile) {
        ConverterAdviser<L> adviser = new ConverterAdviser<>();
        adviser.setEncryptor(createEncryptor(profile));
        return adviser;
    }

    static <K> JdbcPersistenceBuilder<K> apply(JdbcPersistenceBuilder<K> builder, PersistenceProfile profile) {
        String secret = normalizedSecret(profile);
        if (secret == null) {
            return builder;
        }
        return switch (normalizedMode(profile)) {
            case NONE -> builder;
            case MANAGED_DEFAULT -> builder.encryptionSecret(secret);
            case LEGACY_AES_ECB -> builder.encryptionSecret(secret)
                    .encryptionAlgorithm("AES")
                    .encryptionTransformation("AES/ECB/PKCS5Padding")
                    .encryptionKeyLength(16);
        };
    }

    static <K> RedisPersistenceBuilder<K> apply(RedisPersistenceBuilder<K> builder, PersistenceProfile profile) {
        String secret = normalizedSecret(profile);
        if (secret == null) {
            return builder;
        }
        return switch (normalizedMode(profile)) {
            case NONE -> builder;
            case MANAGED_DEFAULT -> builder.encryptionSecret(secret);
            case LEGACY_AES_ECB -> builder.encryptionSecret(secret)
                    .encryptionAlgorithm("AES")
                    .encryptionTransformation("AES/ECB/PKCS5Padding")
                    .encryptionKeyLength(16);
        };
    }

    static boolean isEncryptedHint(String hint) {
        return hint != null && hint.startsWith("__##");
    }

    static boolean canDecrypt(PersistenceProfile profile) {
        return normalizedMode(profile) != PayloadEncryptionMode.NONE && normalizedSecret(profile) != null;
    }

    static String encryptedPayloadMessage(PersistenceProfile profile) {
        return canDecrypt(profile) ? "<decryption failed>" : "<encrypted payload>";
    }

    static String encryptedFieldMessage(PersistenceProfile profile) {
        return canDecrypt(profile) ? "<decryption failed>" : "<encrypted>";
    }

    private static EncryptingConverter createEncryptor(PersistenceProfile profile) {
        String secret = normalizedSecret(profile);
        if (secret == null) {
            return null;
        }
        EncryptingConverterBuilder builder = new EncryptingConverterBuilder().encryptionSecret(secret);
        return switch (normalizedMode(profile)) {
            case NONE -> null;
            case MANAGED_DEFAULT -> builder.get();
            case LEGACY_AES_ECB -> builder.encryptionAlgorithm("AES")
                    .encryptionTransformation("AES/ECB/PKCS5Padding")
                    .encryptionKeyLength(16)
                    .get();
        };
    }

    private static PayloadEncryptionMode normalizedMode(PersistenceProfile profile) {
        return profile == null || profile.payloadEncryptionMode() == null
                ? PayloadEncryptionMode.NONE
                : profile.payloadEncryptionMode();
    }

    private static String normalizedSecret(PersistenceProfile profile) {
        if (profile == null || profile.encryptionSecret() == null) {
            return null;
        }
        String trimmed = profile.encryptionSecret().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
