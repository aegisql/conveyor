package com.aegisql.conveyor.persistence.jdbc.archive;

import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class DeleteArchiver.
 *
 * @param <K> the key type
 */
public class DeleteArchiver<K> extends AbstractJdbcArchiver<K> {
	

	/**
	 * Instantiates a new delete archiver.
	 *
	 * @param engine the engine
	 */
	public DeleteArchiver(EngineDepo<K> engine) {
		super(engine);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByIds(ids);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByCartKeys(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromCompletedLog(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
	@Override
	public void archiveAll() {
		engine.deleteAllParts();
		engine.deleteAllCompletedLog();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		engine.deleteExpiredParts();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.archive.AbstractJdbcArchiver#toString()
	 */
	@Override
	public String toString() {
		return "Delete records from "+engine;
	}
	
}
