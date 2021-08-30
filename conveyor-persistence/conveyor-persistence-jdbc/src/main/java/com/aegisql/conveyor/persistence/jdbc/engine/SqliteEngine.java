package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

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
	public SqliteEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		super(keyClass,connectionFactory);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > 0 AND EXPIRATION_TIME < strftime('%s','now')*1000";
	}

	@Override
	protected void init() {
		setDriver("org.sqlite.JDBC");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:sqlite:{database}");
		setConnectionUrlTemplate("jdbc:sqlite:{database}");
	}

}
