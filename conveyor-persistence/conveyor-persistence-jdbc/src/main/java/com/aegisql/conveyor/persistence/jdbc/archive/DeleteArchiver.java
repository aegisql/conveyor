package com.aegisql.conveyor.persistence.jdbc.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class DeleteArchiver<K> extends AbstractJdbcArchiver<K> {
	

	public DeleteArchiver(Class<K> keyClass, String partTable, String completedTable) {
		super(keyClass,partTable,completedTable);
	}
	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		try(Statement ps = conn.createStatement()) {
			ps.execute(deleteFromPartsByIdSql.replace("?", quoteLong(ids)));
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
			ps.execute(deleteFromPartsByCartKeySql.replace("?", quote(keys)));
		} catch (SQLException e) {
			LOG.error("archiveKeys failure",e);
			throw new PersistenceException("archiveKeys failure",e);
		}
	}

	@Override
	public void archiveCompleteKeys(Connection conn, Collection<K> keys) {
		if(keys.isEmpty()) {
			return;
		}
		try(Statement ps = conn.createStatement()) {
			ps.execute(deleteFromCompletedSql.replace("?", quote(keys)));
		} catch (SQLException e) {
			LOG.error("archiveCompleteKeys failure",e);
			throw new PersistenceException("archiveCompleteKeys failure",e);
		}
	}

	@Override
	public void archiveAll(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(deleteAllPartsSql)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveAll parts failure",e);
			throw new PersistenceException("archiveAll parts failure",e);
		}
		try(PreparedStatement ps = conn.prepareStatement(deleteAllCompletedSql)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveAll completed failure",e);
			throw new PersistenceException("archiveAll completed failure",e);
		}
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(deleteExpiredPartsSql)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveExpiredParts failure",e);
			throw new PersistenceException("archiveExpiredParts failure",e);
		}
	}

	@Override
	public String toString() {
		return "Delete records from "+partTable+" and "+completedLogTable;
	}
	
}
