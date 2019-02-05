package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;

/**
 * The Class PersistenceArchiver.
 *
 * @param <K> the key type
 */
public class PersistenceArchiver<K> implements Archiver<K> {
	Connection conn;
	private final static Logger LOG = LoggerFactory.getLogger(PersistenceArchiver.class);

	private final Class<K> keyClass;
	private final String partTable;
	private final String completedTable;
	private final Archiver<K> deleteArchiver;
	private Persistence<K> persistence;
	private final Persistence<K> archivePersistence;
	private CartToBytesConverter<K, ?, ?> converter;
	private final String getExpiredParts;
	
	public PersistenceArchiver(Class<K> keyClass, String partTable, String completedTable, Persistence<K> persistence,
			ConverterAdviser<?> adviser,Archiver<K> deleteArchiver) {
		this.archivePersistence = persistence;
		this.partTable = partTable;
		this.completedTable = completedTable;
		this.keyClass = keyClass;
		this.deleteArchiver = deleteArchiver;
		this.converter = new CartToBytesConverter<>(adviser);
		this.getExpiredParts = "SELECT"
				+" CART_KEY"
				+",CART_VALUE"
				+",CART_LABEL"
				+",CREATION_TIME"
				+",EXPIRATION_TIME"
				+",LOAD_TYPE"
				+",CART_PROPERTIES"
				+",VALUE_TYPE"
				+" FROM " + partTable + " WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		Collection<Cart<K, ?, Object>> parts = persistence.getParts(ids);
		if(parts != null) {
			parts.forEach(cart->{
				archivePersistence.savePart(archivePersistence.nextUniquePartId(), cart);
			});
			LOG.debug("Archived parts successfully. About to delete data from {}", partTable);
			deleteArchiver.archiveParts(ids);
		}		
	}

	@Override
	public void archiveKeys( Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}		
		ArrayList<Long> ids = new ArrayList<>();
		for(K key:keys) {
			ids.addAll(persistence.getAllPartIds(key));
		}
		archiveParts( ids);
		LOG.debug("Archived parts for keys successfully. About to delete data from {}", partTable);
		deleteArchiver.archiveKeys( keys);

	}

	@Override
	public void archiveCompleteKeys( Collection<K> keys) {
		if (keys == null || keys.isEmpty()) {
			return;
		}
		// nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys( keys);
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
			LOG.debug("Archived expired parts successfully. {}", partTable);
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
			LOG.debug("Archived all parts successfully. {}", partTable);
		}		
	}

	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}

	@Override
	public String toString() {
		return "PersistenceArchiver [keyClass=" + keyClass + ", partTable=" + partTable + ", completedTable="
				+ completedTable + ", archivePersistence=" + archivePersistence + "]";
	}

	
	
}
