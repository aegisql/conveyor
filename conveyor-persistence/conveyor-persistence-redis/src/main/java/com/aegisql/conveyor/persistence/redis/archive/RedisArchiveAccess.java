package com.aegisql.conveyor.persistence.redis.archive;

import java.util.Collection;

public interface RedisArchiveAccess<K> {
    void deleteParts(Collection<Long> ids);

    void deleteKeys(Collection<K> keys);

    void deleteCompletedKeys(Collection<K> keys);

    void deleteExpiredParts();

    void deleteAll();

    Collection<Long> expiredPartIds();

    Collection<Long> activePartIds();

    Collection<Long> staticPartIds();
}
