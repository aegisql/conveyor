package com.aegisql.conveyor.persistence.core;

import java.sql.Connection;

public interface JdbcTools {

	Connection getConnection();
	
	boolean schemaExists();
	boolean partsTableExists();
	boolean keyLogTableExists();
	
	String crateSchemaQuery();
	String createPartsTableQuery();
	String createKeyLogTableQuery();
	
	String insertPartsTableQuery();
	String insertKeyLogTableQuery();

	String archivePartsTableQuery();
	String archiveKeyLogTableQuery();
	
	String selectPartsTableQuery();
	String selectKeyLogTableQuery();

}
