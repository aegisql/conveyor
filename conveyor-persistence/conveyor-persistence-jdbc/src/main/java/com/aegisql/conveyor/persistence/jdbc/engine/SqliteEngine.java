package com.aegisql.conveyor.persistence.jdbc.engine;

public class SqliteEngine <K> extends GenericEngine<K> {
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
	
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > 0 AND EXPIRATION_TIME < strftime('%s','now')*1000";
	}

}
