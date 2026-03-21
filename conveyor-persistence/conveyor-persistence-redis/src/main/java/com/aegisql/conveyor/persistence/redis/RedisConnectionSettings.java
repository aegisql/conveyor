package com.aegisql.conveyor.persistence.redis;

import java.util.Objects;

final class RedisConnectionSettings {

    private final String redisUri;
    private final Integer maxTotal;
    private final Integer maxIdle;
    private final Integer minIdle;
    private final Integer connectionTimeoutMillis;
    private final Integer socketTimeoutMillis;
    private final Integer blockingSocketTimeoutMillis;
    private final Integer database;
    private final String clientName;
    private final String user;
    private final String password;
    private final Boolean ssl;

    RedisConnectionSettings(
            String redisUri,
            Integer maxTotal,
            Integer maxIdle,
            Integer minIdle,
            Integer connectionTimeoutMillis,
            Integer socketTimeoutMillis,
            Integer blockingSocketTimeoutMillis,
            Integer database,
            String clientName,
            String user,
            String password,
            Boolean ssl
    ) {
        this.redisUri = Objects.requireNonNull(redisUri, "redisUri must not be null");
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.socketTimeoutMillis = socketTimeoutMillis;
        this.blockingSocketTimeoutMillis = blockingSocketTimeoutMillis;
        this.database = database;
        this.clientName = clientName;
        this.user = user;
        this.password = password;
        this.ssl = ssl;
    }

    String redisUri() {
        return redisUri;
    }

    Integer maxTotal() {
        return maxTotal;
    }

    Integer maxIdle() {
        return maxIdle;
    }

    Integer minIdle() {
        return minIdle;
    }

    Integer connectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    Integer socketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    Integer blockingSocketTimeoutMillis() {
        return blockingSocketTimeoutMillis;
    }

    Integer database() {
        return database;
    }

    String clientName() {
        return clientName;
    }

    String user() {
        return user;
    }

    String password() {
        return password;
    }

    Boolean ssl() {
        return ssl;
    }

    boolean hasCustomPoolConfig() {
        return maxTotal != null || maxIdle != null || minIdle != null;
    }

    boolean hasCustomClientConfig() {
        return connectionTimeoutMillis != null
                || socketTimeoutMillis != null
                || blockingSocketTimeoutMillis != null
                || database != null
                || clientName != null
                || user != null
                || password != null
                || ssl != null;
    }
}
