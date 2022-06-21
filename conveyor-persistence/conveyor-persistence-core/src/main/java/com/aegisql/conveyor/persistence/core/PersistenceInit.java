package com.aegisql.conveyor.persistence.core;

public interface PersistenceInit <K> {

	void initDatabase(String database);
	void initSchema(String schema);
	void initPartPersistence(Class<K> keyClass, String partTableName);
	void initLogPersistence(Class<K> keyClass, String partTableName);
	
	static <K> PersistenceInit<K> NO_ACTION(Class<K> keyClass) {
		return new PersistenceInit<>() {
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
