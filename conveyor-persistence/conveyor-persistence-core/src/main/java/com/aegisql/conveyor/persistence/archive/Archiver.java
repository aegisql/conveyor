package com.aegisql.conveyor.persistence.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.core.Persistence;

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
	public void archiveParts(Collection<Long> ids);
	
	/**
	 * Archive keys.
	 *
	 * @param conn the conn
	 * @param keys the keys
	 */
	public void archiveKeys(Collection<K> keys);
	
	/**
	 * Archive complete keys.
	 *
	 * @param conn the conn
	 * @param keys the keys
	 */
	public void archiveCompleteKeys(Collection<K> keys);
	
	/**
	 * Archive expired parts.
	 *
	 * @param conn the conn
	 */
	public void archiveExpiredParts();
	
	/**
	 * Archive all.
	 *
	 * @param conn the conn
	 */
	public void archiveAll();
	
	/**
	 * Sets the persistence.
	 *
	 * @param persistence the new persistence
	 */
	public void setPersistence(Persistence<K> persistence);

}

