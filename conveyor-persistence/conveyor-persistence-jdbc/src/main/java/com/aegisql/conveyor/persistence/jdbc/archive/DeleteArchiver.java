package com.aegisql.conveyor.persistence.jdbc.archive;

import java.util.Collection;

import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

public class DeleteArchiver<K> extends AbstractJdbcArchiver<K> {
	

	public DeleteArchiver(EngineDepo<K> engine) {
		super(engine);
	}
	@Override
	public void archiveParts(Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByIds(ids);
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByCartKeys(keys);
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromCompletedLog(keys);
	}

	@Override
	public void archiveAll() {
		engine.deleteAllParts();
		engine.deleteAllCompletedLog();
	}

	@Override
	public void archiveExpiredParts() {
		engine.deleteExpiredParts();
	}

	@Override
	public String toString() {
		return "Delete records from "+engine;
	}
	
}
