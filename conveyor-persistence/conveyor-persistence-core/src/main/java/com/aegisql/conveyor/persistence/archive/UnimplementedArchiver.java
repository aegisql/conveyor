package com.aegisql.conveyor.persistence.archive;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;

import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class UnimplementedArchiver.
 *
 * @param <K> the key type
 */
public class UnimplementedArchiver<K> implements Archiver<K> {

	/**
	 * Instantiates a new unimplemented archiver.
	 */
	public UnimplementedArchiver() {
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {throw new PersistenceException("Unimplemented archiver");}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {throw new PersistenceException("Unimplemented archiver");}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {throw new PersistenceException("Unimplemented archiver");}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
	@Override
	public void archiveAll() {throw new PersistenceException("Unimplemented archiver");}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#setPersistence(com.aegisql.conveyor.persistence.core.Persistence)
	 */
	@Override
	public void setPersistence(Persistence<K> persistence) {}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UnimplementedArchiver: throws exception on any method call";
	}
	
	

}
