package com.aegisql.conveyor.persistence.ui.backend;

import com.aegisql.conveyor.persistence.ui.model.PersistenceProfile;

public final class PersistenceBackendFactory {

    private static final PersistenceBackend JDBC_BACKEND = new JdbcPersistenceBackend();
    private static final PersistenceBackend REDIS_BACKEND = new RedisPersistenceBackend();

    private PersistenceBackendFactory() {
    }

    public static PersistenceBackend forProfile(PersistenceProfile profile) {
        return profile.normalized().kind().isRedis() ? REDIS_BACKEND : JDBC_BACKEND;
    }
}
