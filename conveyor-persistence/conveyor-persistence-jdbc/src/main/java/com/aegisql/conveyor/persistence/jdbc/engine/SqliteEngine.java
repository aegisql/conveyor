package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

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
	public SqliteEngine(Class<K> keyClass) {
		super(
				keyClass,
				"org.sqlite.JDBC",
				"",
				"",
				"jdbc:sqlite:{database}",
				"jdbc:sqlite:{database}"
				);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > 0 AND EXPIRATION_TIME < strftime('%s','now')*1000";
	}

	@Override
	public DataSource getDataSource() {
		return new SQLiteDataSource();
	}
}
