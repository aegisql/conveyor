package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistenceException;

public class SetArchivedArchiver<K> implements Archiver<K> {
	
	private final static Logger LOG = LoggerFactory.getLogger(SetArchivedArchiver.class);

	private final String deleteFromPartsById;
	private final String deleteFromPartsByCartKey;
	private final String deleteFromCompleted;
	
	private final String deleteExpiredParts;
	private final String deleteAllParts;
	private final String deleteAllCompleted;
	
	private final String q;

	public SetArchivedArchiver(Class<K> keyClass, String partTable, String completedTable) {
		deleteFromPartsById      = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE ID IN(?)";
		deleteFromPartsByCartKey = "UPDATE "+partTable + " SET ARCHIVED = 1 WHERE CART_KEY IN(?)";
		deleteFromCompleted      = "DELETE FROM "+completedTable + " WHERE CART_KEY IN(?)";
		
		deleteExpiredParts       = "UPDATE "+partTable+" SET ARCHIVED = 1 WHERE EXPIRATION_TIME > TIMESTAMP('19710101000000') AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
		deleteAllParts           = "UPDATE "+partTable+" SET ARCHIVED = 1";
		deleteAllCompleted       = "DELETE FROM "+completedTable;
		
		if(Number.class.isAssignableFrom(keyClass)) {
			q = "";
		} else {
			q = "'";
		}
	}


	@Override
	public void archiveParts(Connection conn, Collection<Long> ids) {
		if(ids.isEmpty()) {
			return;
		}
		try(Statement ps = conn.createStatement()) {
			StringBuilder sb = new StringBuilder();
			ids.forEach(id->sb.append(id).append(","));
			if(sb.lastIndexOf(",") > 0) {
				sb.deleteCharAt(sb.lastIndexOf(","));
			}
			ps.execute(deleteFromPartsById.replace("?", sb.toString()));
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
			StringBuilder sb = new StringBuilder();
			keys.forEach(id->{
				sb.append(q).append(id).append(q).append(",");
				});
			if(sb.lastIndexOf(",") > 0) {
				sb.deleteCharAt(sb.lastIndexOf(","));
			}
			ps.execute(deleteFromPartsByCartKey.replace("?", sb.toString()));
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
			StringBuilder sb = new StringBuilder();
			keys.forEach(id->{
				sb.append(q).append(id).append(q).append(",");
				});
			if(sb.lastIndexOf(",") > 0) {
				sb.deleteCharAt(sb.lastIndexOf(","));
			}
			ps.execute(deleteFromCompleted.replace("?", sb.toString()));
		} catch (SQLException e) {
			LOG.error("archiveCompleteKeys failure",e);
			throw new PersistenceException("archiveCompleteKeys failure",e);
		}
	}

	@Override
	public void archiveAll(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(deleteAllParts)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveAll parts failure",e);
			throw new PersistenceException("archiveAll parts failure",e);
		}
		try(PreparedStatement ps = conn.prepareStatement(deleteAllCompleted)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveAll completed failure",e);
			throw new PersistenceException("archiveAll completed failure",e);
		}
	}

	@Override
	public void archiveExpiredParts(Connection conn) {
		try(PreparedStatement ps = conn.prepareStatement(deleteExpiredParts)) {
			ps.execute();
		} catch (SQLException e) {
			LOG.error("archiveExpiredParts failure",e);
			throw new PersistenceException("archiveExpiredParts failure",e);
		}
	}


	@Override
	public void setPersistence(Persistence<K> persistence) {}

}
