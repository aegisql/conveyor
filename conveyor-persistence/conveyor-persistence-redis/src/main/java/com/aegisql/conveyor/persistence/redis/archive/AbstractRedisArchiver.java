package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.utils.PersistUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class AbstractRedisArchiver<K> implements Archiver<K> {

    protected final RedisArchiveAccess<K> archiveAccess;
    protected Persistence<K> persistence;

    protected AbstractRedisArchiver(RedisArchiveAccess<K> archiveAccess) {
        this.archiveAccess = archiveAccess;
    }

    @Override
    public void setPersistence(Persistence<K> persistence) {
        this.persistence = persistence;
    }

    protected Collection<Collection<Long>> balanceIds(Collection<Long> ids, int batchSize) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return PersistUtils.balanceIdList(new LinkedHashSet<>(ids), Math.max(1, batchSize));
    }
}
