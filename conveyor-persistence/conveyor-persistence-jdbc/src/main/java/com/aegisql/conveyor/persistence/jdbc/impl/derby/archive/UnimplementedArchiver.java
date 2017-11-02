package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.Collection;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class UnimplementedArchiver<K> implements Archiver<K> {

	public UnimplementedArchiver() {
	}

	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveExpiredParts(Connection conn) {throw new PersistenceException("Unimplemented archiver");}

	@Override
	public void archiveAll(Connection conn) {throw new PersistenceException("Unimplemented archiver");}

}
