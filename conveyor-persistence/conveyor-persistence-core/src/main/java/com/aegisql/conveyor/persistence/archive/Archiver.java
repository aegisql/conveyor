package com.aegisql.conveyor.persistence.archive;

import com.aegisql.conveyor.persistence.core.Persistence;

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
	 * @param ids the ids
	 */
	void archiveParts(Collection<Long> ids);
	
	/**
	 * Archive keys.
	 *
	 * @param keys the keys
	 */
	void archiveKeys(Collection<K> keys);
	
	/**
	 * Archive complete keys.
	 *
	 * @param keys the keys
	 */
	void archiveCompleteKeys(Collection<K> keys);
	
	/**
	 * Archive expired parts.
	 */
	void archiveExpiredParts();
	
	/**
	 * Archive all.
	 */
	void archiveAll();
	
	/**
	 * Sets the persistence.
	 *
	 * @param persistence the new persistence
	 */
	void setPersistence(Persistence<K> persistence);

}

