package com.aegisql.conveyor.persistence.jdbc;

import java.sql.Connection;
import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Interface Archiver.
 *
 * @param <K> the key type
 */
public interface Archiver<K> {
	
	/**
	 * Archive parts.
	 *
	 * @param conn the conn
	 * @param ids the ids
	 */
	public void archiveParts(Connection conn,Collection<Long> ids);
	
	/**
	 * Archive keys.
	 *
	 * @param conn the conn
	 * @param keys the keys
	 */
	public void archiveKeys(Connection conn,Collection<K> keys);
	
	/**
	 * Archive complete keys.
	 *
	 * @param conn the conn
	 * @param keys the keys
	 */
	public void archiveCompleteKeys(Connection conn,Collection<K> keys);
	
	/**
	 * Archive expired parts.
	 *
	 * @param conn the conn
	 */
	public void archiveExpiredParts(Connection conn);
	
	/**
	 * Archive all.
	 *
	 * @param conn the conn
	 */
	public void archiveAll(Connection conn);

}

