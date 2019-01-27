package com.aegisql.conveyor.persistence.core;

public interface PersistenceInit <K> {

	public void initDatabase(String database);
	public void initSchema(String schema);
	public void initPartPersistence(Class<K> keyClass, String partTableName);
	public void initLogPersistence(Class<K> keyClass, String partTableName);
	
	public static <K> PersistenceInit<K> NO_ACTION(Class<K> keyClass) {
		return new PersistenceInit<K>() {
			@Override
			public void initDatabase(String database) {
			}
			@Override
			public void initSchema(String schema) {
			}

			@Override
			public void initPartPersistence(Class<K> keyClass, String partTableName) {
			}

			@Override
			public void initLogPersistence(Class<K> keyClass, String partTableName) {
			}
		};
	}
	
}
