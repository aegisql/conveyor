package com.aegisql.conveyor.persistence.redis.archive;

import java.util.Collection;
import java.util.LinkedHashSet;

public class DeleteRedisArchiver<K> extends AbstractRedisArchiver<K> {

    public DeleteRedisArchiver(RedisArchiveAccess<K> archiveAccess) {
        super(archiveAccess);
    }

    @Override
    public void archiveParts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        archiveAccess.deleteParts(new LinkedHashSet<>(ids));
    }

    @Override
    public void archiveKeys(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (K key : keys) {
            if (key != null) {
                ids.addAll(persistence.getAllPartIds(key));
            }
        }
        archiveAccess.deleteParts(ids);
    }

    @Override
    public void archiveCompleteKeys(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        archiveAccess.deleteCompletedKeys(keys);
    }

    @Override
    public void archiveExpiredParts() {
        archiveAccess.deleteParts(archiveAccess.expiredPartIds());
    }

    @Override
    public void archiveAll() {
        archiveAccess.deleteAll();
    }
}
