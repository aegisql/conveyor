package com.aegisql.conveyor.persistence.jdbc.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.core.PersistenceInit;

public abstract class AbstractJdbcPersistenceInit<K> implements PersistenceInit<K> {

	protected Connection connection;
	protected String initDatabaseSql;
	protected String initSchemaSql;
	protected String initPartTableSql;
	protected String initLogTableSql;

	protected List<String> initPArtIndexesSql = new ArrayList<String>();
	
	@Override
	public void initSchema(String schema) {
		if(schema != null) {
		}
	}

	@Override
	public void initPartPersistence(Class<K> keyClass, String partTableName) {
		if(initPartTableSql != null) {
			try(Statement st = connection.createStatement() ) {
				st.executeQuery(initPartTableSql);
			}catch (SQLException e) {
				throw new PersistenceException("Failed init part table init", e);
			}
		}
	}

	@Override
	public void initLogPersistence(Class<K> keyClass, String partTableName) {
		if(initLogTableSql != null) {
			
		}
	}

	@Override
	public void initDatabase(String database) {
		if(database != null) {
			
		}
	}
	
	abstract public String keyClassToSqlType(Class<K> keyClass);


}
