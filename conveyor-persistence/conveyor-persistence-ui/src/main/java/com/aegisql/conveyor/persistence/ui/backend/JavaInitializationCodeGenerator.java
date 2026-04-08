package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.ui.model.PayloadEncryptionMode;
import com.aegisql.conveyor.persistence.ui.model.PersistenceKind;
import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;

final class JavaInitializationCodeGenerator {

    private JavaInitializationCodeGenerator() {
    }

    static String jdbcInitializationCode(PersistenceProfile profile, String effectiveDatabase) {
        PersistenceProfile normalized = profile.normalized();
        String keyClassLiteral = classLiteral(normalized.keyClassName());
        String builderType = "JdbcPersistenceBuilder<" + sourceTypeName(normalized.keyClassName()) + ">";
        StringBuilder code = new StringBuilder();
        code.append("import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;\n\n");
        code.append("public final class InitPersistenceExample {\n\n");
        code.append("    private InitPersistenceExample() {\n");
        code.append("    }\n\n");
        code.append("    public static void main(String[] args) {\n");
        if (normalized.kind() == PersistenceKind.DERBY && normalized.database() != null && !normalized.database().equals(effectiveDatabase)) {
            code.append("        // Derby uses the selected home directory plus the database name to build the actual target path.\n");
        }
        code.append("        ").append(builderType).append(" builder = JdbcPersistenceBuilder\n");
        code.append("                .presetInitializer(\"").append(javaEscape(normalized.kind().jdbcType())).append("\", ")
                .append(keyClassLiteral).append(")\n");
        appendJdbcBuilderSettings(code, normalized, effectiveDatabase);
        code.append("                .autoInit(false);\n\n");
        code.append("        builder.init();\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }

    static String redisInitializationCode(PersistenceProfile profile) {
        PersistenceProfile normalized = profile.normalized();
        StringBuilder code = new StringBuilder();
        code.append("import com.aegisql.conveyor.persistence.core.Persistence;\n");
        code.append("import com.aegisql.conveyor.persistence.redis.RedisPersistenceBuilder;\n\n");
        code.append("public final class InitPersistenceExample {\n\n");
        code.append("    private InitPersistenceExample() {\n");
        code.append("    }\n\n");
        code.append("    public static void main(String[] args) throws Exception {\n");
        code.append("        RedisPersistenceBuilder<Object> builder = new RedisPersistenceBuilder<Object>(\"")
                .append(javaEscape(normalized.persistenceName())).append("\")\n");
        code.append("                .redisUri(\"").append(javaEscape(normalized.redisUri())).append("\")\n");
        if (normalized.user() != null) {
            code.append("                .user(\"").append(javaEscape(normalized.user())).append("\")\n");
        }
        if (normalized.password() != null) {
            code.append("                .password(\"<redis-password>\")\n");
        }
        appendEncryptionSettings(code, normalized);
        code.append("                .autoInit(true);\n\n");
        code.append("        try (Persistence<Object> ignored = builder.build()) {\n");
        code.append("            // Redis namespace initialized here.\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }

    private static void appendJdbcBuilderSettings(StringBuilder code, PersistenceProfile profile, String effectiveDatabase) {
        if (profile.host() != null) {
            code.append("                .host(\"").append(javaEscape(profile.host())).append("\")\n");
        }
        if (profile.port() != null) {
            code.append("                .port(").append(profile.port()).append(")\n");
        }
        if (effectiveDatabase != null) {
            code.append("                .database(\"").append(javaEscape(effectiveDatabase)).append("\")\n");
        }
        if (profile.schema() != null) {
            code.append("                .schema(\"").append(javaEscape(profile.schema())).append("\")\n");
        }
        if (profile.partTable() != null) {
            code.append("                .partTable(\"").append(javaEscape(profile.partTable())).append("\")\n");
        }
        if (profile.completedLogTable() != null) {
            code.append("                .completedLogTable(\"").append(javaEscape(profile.completedLogTable())).append("\")\n");
        }
        if (profile.user() != null) {
            code.append("                .user(\"").append(javaEscape(profile.user())).append("\")\n");
        }
        if (profile.password() != null) {
            code.append("                .password(\"<jdbc-password>\")\n");
        }
        appendEncryptionSettings(code, profile);
    }

    private static void appendEncryptionSettings(StringBuilder code, PersistenceProfile profile) {
        if (profile.payloadEncryptionMode() == null || profile.payloadEncryptionMode() == PayloadEncryptionMode.NONE || profile.encryptionSecret() == null) {
            return;
        }
        code.append("                .encryptionSecret(\"<encryption-secret>\")\n");
        if (profile.payloadEncryptionMode() == PayloadEncryptionMode.LEGACY_AES_ECB) {
            code.append("                .encryptionAlgorithm(\"AES\")\n");
            code.append("                .encryptionTransformation(\"AES/ECB/PKCS5Padding\")\n");
            code.append("                .encryptionKeyLength(16)\n");
        }
    }

    private static String classLiteral(String keyClassName) {
        if (keyClassName == null || keyClassName.isBlank()) {
            return "Object.class";
        }
        return switch (keyClassName) {
            case "boolean", "byte", "short", "int", "long", "float", "double", "char" -> keyClassName + ".class";
            default -> {
                yield sourceTypeName(keyClassName) + ".class";
            }
        };
    }

    private static String sourceTypeName(String keyClassName) {
        if (keyClassName == null || keyClassName.isBlank()) {
            return "Object";
        }
        String normalized = keyClassName.replace('$', '.');
        if (normalized.startsWith("java.lang.")) {
            return normalized.substring("java.lang.".length());
        }
        return normalized;
    }

    private static String javaEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
