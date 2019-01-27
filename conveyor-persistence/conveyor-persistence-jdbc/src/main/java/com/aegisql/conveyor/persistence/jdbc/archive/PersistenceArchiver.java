package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.core.Persistence;

/**
 * The Class PersistenceArchiver.
 *
 * @param <K> the key type
 */
public class PersistenceArchiver<K> extends AbstractJdbcArchiver<K> {
	
	
	private final DeleteArchiver<K> deleteArchiver;
	private final Persistence<K> archivePersistence;

	public PersistenceArchiver(Class<K> keyClass, String partTable, String completedTable, Persistence<K> persistence) {
		super(keyClass, partTable, completedTable);
		this.deleteArchiver = new DeleteArchiver<>(keyClass, partTable, completedTable);
		this.archivePersistence = persistence;
	}

	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		Collection<Cart<K, ?, Object>> parts = persistence.getParts(ids);
		if(parts != null) {
			parts.forEach(cart -> archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart) );
			LOG.debug("Archived parts successfully. About to delete data from {}", partTable);
			deleteArchiver.archiveParts(conn, ids);
		}		
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}		
		ArrayList<Long> ids = new ArrayList<>();
		for(K key:keys) {
			ids.addAll(persistence.getAllPartIds(key));
		}
		archiveParts(conn, ids);
		LOG.debug("Archived parts for keys successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveKeys(conn, keys);

	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		Collection<Cart<K, ?, Object>> parts = persistence.getExpiredParts();
		Collection<K> keys = new ArrayList<>();
		if(parts != null) {
			parts.forEach(cart->{
				archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
				keys.add(cart.getKey());
			});
			archiveKeys(conn, keys);
			LOG.debug("Archived expired parts successfully. {}", partTable);
		}		
	}

	@Override
	public void archiveAll(Connection conn) {
		Collection<Cart<K, ?, Object>> parts = persistence.getAllParts();
		Collection<K> keys = new ArrayList<>();
		if(parts != null) {
			parts.forEach(cart->{
				archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
				keys.add(cart.getKey());
			});
			archiveKeys(conn, keys);
			LOG.debug("Archived all parts successfully. {}", partTable);
		}		
	}

	@Override
	public String toString() {
		return "PersistenceArchiver [keyClass=" + keyClass + ", partTable=" + partTable + ", completedTable="
				+ completedLogTable + ", archivePersistence=" + archivePersistence + "]";
	}

	
	
}
