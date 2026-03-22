package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;

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
}
