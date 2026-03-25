package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class PersistenceRedisArchiver<K> extends AbstractRedisArchiver<K> {

    private final Persistence<K> archivePersistence;

    public PersistenceRedisArchiver(RedisArchiveAccess<K> archiveAccess, Persistence<K> archivePersistence) {
        super(archiveAccess);
        this.archivePersistence = archivePersistence;
    }

    @Override
    public void archiveParts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Collection<Long> bucket : balanceIds(ids, persistence.getMaxArchiveBatchSize())) {
            Collection<Cart<K, ?, Object>> parts = persistence.getParts(bucket);
            persist(parts);
            archiveAccess.deleteParts(bucket);
        }
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
        archiveParts(ids);
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
        archiveParts(archiveAccess.expiredPartIds());
    }

    @Override
    public void archiveAll() {
        archiveParts(archiveAccess.activePartIds());
        archiveParts(archiveAccess.staticPartIds());
        Set<K> completedKeys = persistence.getCompletedKeys();
        archiveCompleteKeys(completedKeys);
        archiveAccess.deleteAll();
    }

    private void persist(Collection<Cart<K, ?, Object>> parts) {
        for (Cart<K, ?, Object> cart : parts) {
            archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
        }
    }
}
