package com.aegisql.conveyor.persistence.jdbc.engine.sqlite;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.WrappedExternalDataSource;
import com.aegisql.conveyor.persistence.jdbc.init.JdbcScriptSection;
import org.sqlite.SQLiteConfig;

import java.sql.SQLException;
import java.util.UUID;

/**
 * The Class SqliteEngine.
 *
 * @param <K> the key type
 */
public class SqliteEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new sqlite engine.
	 *
	 * @param keyClass the key class
	 */
	public SqliteEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > 0 AND EXPIRATION_TIME < strftime('%s','now')*1000";
	}

	@Override
	public String defaultDriverClassName() {
		return "org.sqlite.JDBC";
	}

	@Override
	public ConnectionFactory<?> defaultConnectionFactory() {
		SQLiteConfig config = new SQLiteConfig();
		//config.setJournalMode(SQLiteConfig.JournalMode.PERSIST);
		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);

		ConnectionFactory cf = ConnectionFactory.cachingFactoryInstance(f-> new WrappedExternalDataSource(()-> {
            try {
                return config.createConnection(f.getUrl());
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }));
		return cf;
	}

	@Override
	protected void init() {
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:sqlite:{database}");
		setConnectionUrlTemplate("jdbc:sqlite:{database}");
	}

	@Override
	protected String getFieldType(Class<?> fieldClass) {
		if(fieldClass.isEnum()) {
			int maxLength = 0;
			for(Object constant : fieldClass.getEnumConstants()) {
				maxLength = Math.max(maxLength, constant.toString().length());
			}
			return "CHAR(" + maxLength + ") NOT NULL";
		}
		return switch (fieldClass.getName()) {
			case "java.lang.Integer" -> "INT NOT NULL";
			case "java.lang.Long" -> "BIGINT NOT NULL";
			case "java.util.UUID" -> "CHAR(36) NOT NULL";
			default -> "VARCHAR(255) NOT NULL";
		};
	}

	@Override
	protected JdbcScriptSection getCreateDatabaseScriptSection(String database) {
		if (database == null || database.isBlank()) {
			return null;
		}
		return JdbcScriptSection.note(
				"Create database",
				"SQLite creates the database file when the client opens it.",
				"Point your client to " + database + " before running the remaining statements."
		);
	}

	@Override
	protected String getCreatePartTableSql(String partTable) {
		return super.getCreatePartTableSql(partTable).replaceFirst("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
	}

	@Override
	protected String getCreatePartTableIndexSql(String partTable) {
		return "CREATE INDEX IF NOT EXISTS " + indexName(partTable) + " ON " + partTable + "(" + CART_KEY + ")";
	}

	@Override
	protected String getCreateUniqPartTableIndexSql(String partTable, java.util.List<String> fields) {
		String indexName = indexName(partTable, fields);
		return "CREATE UNIQUE INDEX IF NOT EXISTS " + indexName + " ON " + partTable + "(" + String.join(",", fields) + ")";
	}

	@Override
	protected String getCompletedLogTableSql(String completedLogTable) {
		return super.getCompletedLogTableSql(completedLogTable).replaceFirst("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
	}

	@Override
	protected String getDropUniquePartTableIndexSql(String partTable, java.util.List<String> fields) {
		return "DROP INDEX IF EXISTS " + indexName(partTable, fields);
	}

	@Override
	protected String getDropPartTableIndexSql(String partTable) {
		return "DROP INDEX IF EXISTS " + indexName(partTable);
	}

	@Override
	protected String getDropCompletedLogTableSql(String completedLogTable) {
		return "DROP TABLE IF EXISTS " + completedLogTable;
	}

	@Override
	protected String getDropPartTableSql(String partTable) {
		return "DROP TABLE IF EXISTS " + partTable;
	}

	@Override
	protected JdbcScriptSection getDropDatabaseScriptSection(String database) {
		if (database == null || database.isBlank()) {
			return null;
		}
		return JdbcScriptSection.note(
				"Drop database",
				"SQLite database cleanup is a filesystem operation, not SQL.",
				"Delete file " + database + " manually if you want to remove the database."
		);
	}

}
