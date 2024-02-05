package com.aegisql.conveyor.persistence.jdbc.archive;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

import java.util.ArrayList;
import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceArchiver.
 *
 * @param <K> the key type
 */
public class PersistenceArchiver<K> extends AbstractJdbcArchiver<K> {
	
	
	/** The delete archiver. */
	private final DeleteArchiver<K> deleteArchiver;
	
	/** The archive persistence. */
	private final Persistence<K> archivePersistence;

	/**
	 * Instantiates a new persistence archiver.
	 *
	 * @param engine the engine
	 * @param persistence the persistence
	 */
	public PersistenceArchiver(EngineDepo<K> engine, Persistence<K> persistence) {
		super(engine);
		this.deleteArchiver = new DeleteArchiver<>(engine);
		this.archivePersistence = persistence;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		Collection<Cart<K, ?, Object>> parts = persistence.getParts(ids);
		if(parts != null) {
			parts.forEach(cart -> archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart) );
			deleteArchiver.archiveParts(ids);
		}		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
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
		deleteArchiver.archiveKeys(keys);

	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
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
		}		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
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
		}		
	}
	
}
