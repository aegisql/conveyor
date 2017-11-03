package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.archive.Archiver;

public class FileArchiver<K> implements Archiver<K> {
	
	private final static Logger LOG = LoggerFactory.getLogger(FileArchiver.class);
	
	private final Class<K> keyClass;
	private final String partTable;
	private final String completedTable;
	private final DeleteArchiver<K> deleteArchiver;
	
	private Persistence<K> persistence;
	

	public FileArchiver(Class<K> keyClass, String partTable, String completedTable) {
		this.partTable      = partTable;
		this.completedTable = completedTable;
		this.keyClass       = keyClass;
		this.deleteArchiver = new DeleteArchiver<>(keyClass, partTable, completedTable);
	}

	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids == null || ids.isEmpty()) {
			return;
		}
		//TODO - impl body
		LOG.debug("Archived parts successfully. About to delete data from {}",partTable);
		deleteArchiver.archiveParts(conn, ids);
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if(keys == null || keys.isEmpty()) {
			return;
		}
		//TODO - impl body
		LOG.debug("Archived parts for keys successfully. About to delete data from {}",partTable);
		deleteArchiver.archiveKeys(conn, keys);
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		if(keys == null || keys.isEmpty()) {
			return;
		}
		//nothing else required. Just delete completed keys
		deleteArchiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		//TODO - impl body
		LOG.debug("Archived expired parts successfully. About to delete data from {}",partTable);
		deleteArchiver.archiveExpiredParts(conn);
	}

	@Override
	public void archiveAll(Connection conn) {
		// TODO - impl body
		LOG.debug("Archived all parts successfully. About to delete data from {}",partTable);
		deleteArchiver.archiveAll(conn);
	}

	@Override
	public void setPersistence(Persistence<K> persistence) {
		this.persistence = persistence;
	}

}
