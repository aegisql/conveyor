package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.jdbc.archive.Archiver;

public class FileArchiver<K> implements Archiver<K> {
	
	private final static Logger LOG = LoggerFactory.getLogger(FileArchiver.class);

	public FileArchiver(Class<K> keyClass, String partTable, String completedTable) {
		// TODO Auto-generated constructor stub
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

}
