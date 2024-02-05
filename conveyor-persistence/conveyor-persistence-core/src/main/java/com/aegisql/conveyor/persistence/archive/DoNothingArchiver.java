package com.aegisql.conveyor.persistence.archive;

import com.aegisql.conveyor.persistence.core.Persistence;

import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class DoNothingArchiver.
 *
 * @param <K> the key type
 */
public class DoNothingArchiver<K> implements Archiver<K> {

	/**
	 * Instantiates a new do nothing archiver.
	 */
	public DoNothingArchiver() {
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys( Collection<K> keys) {}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
	@Override
	public void archiveAll() {}

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
		return "DoNothingArchiver: no action on archive commands";
	}
	
	

}
