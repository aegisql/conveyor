package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.util.Collection;

import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

public class DeleteArchiver<K> extends AbstractJdbcArchiver<K> {
	

	public DeleteArchiver(EngineDepo<K> engine) {
		super(engine);
	}
	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByIds(ids);
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromPartsByCartKeys(keys);
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.deleteFromCompletedLog(keys);
	}

	@Override
	public void archiveAll(Connection conn) {
		engine.deleteAllParts();
		engine.deleteAllCompletedLog();
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		engine.deleteExpiredParts();
	}

	@Override
	public String toString() {
		return "Delete records from "+engine;
	}
	
}
