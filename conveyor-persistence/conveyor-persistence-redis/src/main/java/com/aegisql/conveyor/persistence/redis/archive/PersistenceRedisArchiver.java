package com.aegisql.conveyor.persistence.redis.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

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
        Collection<Cart<K, ?, Object>> parts = persistence.getParts(new LinkedHashSet<>(ids));
        persist(parts);
        archiveAccess.deleteParts(ids);
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
        Collection<Long> ids = archiveAccess.expiredPartIds();
        Collection<Cart<K, ?, Object>> parts = persistence.getParts(ids);
        persist(parts);
        archiveAccess.deleteParts(ids);
    }

    @Override
    public void archiveAll() {
        ArrayList<Cart<K, ?, Object>> parts = new ArrayList<>(persistence.getAllParts());
        parts.addAll(persistence.getAllStaticParts());
        persist(parts);
        archiveAccess.deleteAll();
    }

    private void persist(Collection<Cart<K, ?, Object>> parts) {
        for (Cart<K, ?, Object> cart : parts) {
            archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
        }
    }
}
