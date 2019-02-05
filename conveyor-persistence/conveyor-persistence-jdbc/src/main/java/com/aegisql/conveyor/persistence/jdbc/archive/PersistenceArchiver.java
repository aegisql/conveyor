package com.aegisql.conveyor.persistence.jdbc.archive;

import java.util.ArrayList;
import java.util.Collection;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

/**
 * The Class PersistenceArchiver.
 *
 * @param <K> the key type
 */
public class PersistenceArchiver<K> extends AbstractJdbcArchiver<K> {
	
	
	private final DeleteArchiver<K> deleteArchiver;
	private final Persistence<K> archivePersistence;

	public PersistenceArchiver(EngineDepo<K> engine, Persistence<K> persistence) {
		super(engine);
		this.deleteArchiver = new DeleteArchiver<>(engine);
		this.archivePersistence = persistence;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		Collection<Cart<K, ?, Object>> parts = persistence.getParts(ids);
		if(parts != null) {
			parts.forEach(cart -> archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart) );
//			LOG.debug("Archived parts successfully. About to delete data from {}", partTable);
			deleteArchiver.archiveParts(ids);
		}		
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}		
		ArrayList<Long> ids = new ArrayList<>();
		for(K key:keys) {
			ids.addAll(persistence.getAllPartIds(key));
		}
		archiveParts(ids);
//		LOG.debug("Archived parts for keys successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveKeys(keys);

	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(keys);
	}

	@Override
	public void archiveExpiredParts() {
		Collection<Cart<K, ?, Object>> parts = persistence.getExpiredParts();
		Collection<K> keys = new ArrayList<>();
		if(parts != null) {
			parts.forEach(cart->{
				archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
				keys.add(cart.getKey());
			});
			archiveKeys(keys);
//			LOG.debug("Archived expired parts successfully. {}", partTable);
		}		
	}

	@Override
	public void archiveAll() {
		Collection<Cart<K, ?, Object>> parts = persistence.getAllParts();
		Collection<K> keys = new ArrayList<>();
		if(parts != null) {
			parts.forEach(cart->{
				archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
				keys.add(cart.getKey());
			});
			archiveKeys(keys);
//			LOG.debug("Archived all parts successfully. {}", partTable);
		}		
	}

	@Override
	public String toString() {
		return "PersistenceArchiver";
	}

	
	
}
