package com.aegisql.conveyor.persistence.jdbc.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

// TODO: Auto-generated Javadoc
/**
 * The Class SetArchivedArchiver.
 *
 * @param <K> the key type
 */
public class SetArchivedArchiver<K> extends AbstractJdbcArchiver<K> {
	
		/** The delete archiver. */
		private final DeleteArchiver<K> deleteArchiver;
	
		/**
		 * Instantiates a new sets the archived archiver.
		 *
		 * @param engine the engine
		 */
		public SetArchivedArchiver(EngineDepo<K> engine) {
			super(engine);
			this.deleteArchiver = new DeleteArchiver<>(engine);
		}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveParts(java.util.Collection)
	 */
	@Override
	public void archiveParts(Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.updatePartsByIds(ids);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveKeys(java.util.Collection)
	 */
	@Override
	public void archiveKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.updatePartsByCartKeys(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveCompleteKeys(java.util.Collection)
	 */
	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		deleteArchiver.archiveCompleteKeys(keys);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveAll()
	 */
	@Override
	public void archiveAll() {
		engine.updateAllParts();
		engine.deleteAllCompletedLog();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.archive.Archiver#archiveExpiredParts()
	 */
	@Override
	public void archiveExpiredParts() {
		engine.updateExpiredParts();
	}

}
