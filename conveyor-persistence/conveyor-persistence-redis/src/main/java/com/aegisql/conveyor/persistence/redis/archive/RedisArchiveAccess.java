package com.aegisql.conveyor.persistence.redis.archive;

import java.util.Collection;

public interface RedisArchiveAccess<K> {
    void deleteParts(Collection<Long> ids);

    void deleteCompletedKeys(Collection<K> keys);

    void deleteAll();

    Collection<Long> expiredPartIds();
}
