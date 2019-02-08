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
	 * @param ids the ids
	 */
	public void archiveParts(Collection<Long> ids);
	
	/**
	 * Archive keys.
	 *
	 * @param keys the keys
	 */
	public void archiveKeys(Collection<K> keys);
	
	/**
	 * Archive complete keys.
	 *
	 * @param keys the keys
	 */
	public void archiveCompleteKeys(Collection<K> keys);
	
	/**
	 * Archive expired parts.
	 */
	public void archiveExpiredParts();
	
	/**
	 * Archive all.
	 */
	public void archiveAll();
	
	/**
	 * Sets the persistence.
	 *
	 * @param persistence the new persistence
	 */
	public void setPersistence(Persistence<K> persistence);

}

