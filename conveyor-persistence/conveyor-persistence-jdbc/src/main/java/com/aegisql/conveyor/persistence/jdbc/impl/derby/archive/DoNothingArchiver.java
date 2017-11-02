package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.util.Collection;

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

}
