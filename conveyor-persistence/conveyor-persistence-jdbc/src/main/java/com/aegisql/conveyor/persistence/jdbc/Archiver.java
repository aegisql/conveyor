package com.aegisql.conveyor.persistence.jdbc;

import java.sql.Connection;
import java.util.Collection;

public interface Archiver<K> {
	public void archiveParts(Connection conn,Collection<Long> ids);
	public void archiveKeys(Connection conn,Collection<K> keys);
	public void archiveCompleteKeys(Connection conn,Collection<K> keys);
	public void archiveExpiredParts(Connection conn);
	public void archiveAll(Connection conn);

}

