package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.converters.CartToBytesConverter;
import com.aegisql.conveyor.persistence.converters.ConverterAdviser;
import com.aegisql.conveyor.persistence.core.Persistence;

public class PersistenceArchiver<K> implements Archiver<K> {
	
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
	public void archiveParts(Connection conn, Collection<Long> ids) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveAll(Connection conn) {
		// TODO Auto-generated method stub
		
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
