package com.aegisql.conveyor.persistence.jdbc.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

public class SetArchivedArchiver<K> extends AbstractJdbcArchiver<K> {
	
		private final DeleteArchiver<K> deleteArchiver;
	
		public SetArchivedArchiver(EngineDepo<K> engine) {
			super(engine);
			this.deleteArchiver = new DeleteArchiver<>(engine);
		}


	@Override
	public void archiveParts(Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.updatePartsByIds(ids);
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.updatePartsByCartKeys(keys);
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		deleteArchiver.archiveCompleteKeys(keys);
	}

	@Override
	public void archiveAll() {
		engine.updateAllParts();
		engine.deleteAllCompletedLog();
	}

	@Override
	public void archiveExpiredParts() {
		engine.updateExpiredParts();
	}

}
