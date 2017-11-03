package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.util.Collection;

import com.aegisql.conveyor.persistence.core.Persistence;

public class DoNothingArchiver<K> implements Archiver<K> {

	public DoNothingArchiver() {
	}

	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {}

	@Override
	public void archiveExpiredParts(Connection conn) {}

	@Override
	public void archiveAll(Connection conn) {}

	@Override
	public void setPersistence(Persistence<K> persistence) {}

}
