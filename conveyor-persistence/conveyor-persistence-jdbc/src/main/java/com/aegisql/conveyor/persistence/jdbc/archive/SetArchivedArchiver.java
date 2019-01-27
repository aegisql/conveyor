package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class SetArchivedArchiver<K> extends AbstractJdbcArchiver<K> {
	
		private final DeleteArchiver<K> deleteArchiver;
	
		public SetArchivedArchiver(Class<K> keyClass, String partTable, String completedTable) {
			super(keyClass, partTable, completedTable);
			this.deleteArchiver = new DeleteArchiver<>(keyClass, partTable, completedTable);
		}


	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		try(Statement ps = conn.createStatement()) {
			ps.execute(updatePartsByIdSql.replace("?", quoteLong(ids)));
		} catch (SQLException e) {
			LOG.error("archiveParts failure",e);
			throw new PersistenceException("archiveParts failure",e);
		}
	}

	@Override
	public void archiveKeys(Connection conn, Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		try(Statement ps = conn.createStatement()) {
			ps.execute(updatePartsByCartKeySql.replace("?", quote(keys)));
		} catch (SQLException e) {
			LOG.error("archiveKeys failure",e);
			throw new PersistenceException("archiveKeys failure",e);
		}
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		deleteArchiver.archiveCompleteKeys(conn, keys);
	}

	@Override
	public void archiveAll(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(updateAllPartsSql)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveAll parts failure",e);
			throw new PersistenceException("archiveAll parts failure",e);
		}
		deleteArchiver.archiveAll(conn);
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(updateExpiredPartsSql)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveExpiredParts failure",e);
			throw new PersistenceException("archiveExpiredParts failure",e);
		}
	}

	@Override
	public String toString() {
		return "Set ARCHIVED=1 in "+partTable+"; delete from "+completedLogTable;
	}
	
}
