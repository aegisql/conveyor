package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.EngineDepo;

public class SetArchivedArchiver<K> extends AbstractJdbcArchiver<K> {
	
		private final DeleteArchiver<K> deleteArchiver;
	
		public SetArchivedArchiver(EngineDepo<K> engine) {
			super(engine);
			this.deleteArchiver = new DeleteArchiver<>(engine);
		}


	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		engine.updatePartsByIds(ids);
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		engine.updatePartsByCartKeys(keys);
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		deleteArchiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveAll(Connection conn) {
		engine.updateAllParts();
		//deleteArchiver.archiveAll(conn);
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		engine.updateExpiredParts();
	}

	@Override
	public String toString() {
		return "Set ARCHIVED=1";
	}
	
}
