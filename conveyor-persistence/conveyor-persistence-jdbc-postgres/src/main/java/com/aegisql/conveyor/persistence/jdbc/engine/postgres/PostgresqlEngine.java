package com.aegisql.conveyor.persistence.jdbc.engine.postgres;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.SQLException;
import java.util.UUID;

// TODO: Auto-generated Javadoc
/**
 * The Class PostgresqlEngine.
 *
 * @param <K> the key type
 */
public class PostgresqlEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new postgresql engine.
	 *
	 * @param keyClass the key class
	 */
	public PostgresqlEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#databaseExists(java.lang.String)
	 */
	@Override
	public boolean databaseExists(String database) {
		String query = "SELECT 1 FROM pg_catalog.pg_database WHERE lower(datname) = lower(?)";
		switchUrlTemplae(connectionUrlTemplateForInitDatabase);
		Boolean hasValue = getScalarValue(query,st->{
			try {
				st.setString(1, database);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		},rs->{
			try {
				return rs.getBoolean(1);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		return hasValue != null && hasValue;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#partTableIndexExists(java.lang.String)
	 */
	@Override
	public boolean partTableIndexExists(String partTable, String indexName) {
		String query = "SELECT 1 FROM pg_catalog. pg_indexes WHERE lower(indexname) = lower(?)";
		switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes);
		Boolean hasValue = getScalarValue(query,st->{
			try {
				st.setString(1, indexName);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		},rs->{
			try {
				return rs.getBoolean(1);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		return hasValue != null && hasValue;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > '1970-01-01 00:00:01' AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}

	public int defaultPort() {
		return 5432;
	}

	@Override
	public String defaultDriverClassName() {
		return "org.postgresql.Driver";
	}
	
	@Override
	protected void init() {
		setField(CART_VALUE, "BYTEA");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setConnectionUrlTemplateForInitDatabase("jdbc:postgresql://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("jdbc:postgresql://{host}:{port}/{database}");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}");
		setConnectionUrlTemplate("jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}");
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
	protected String getCreateSchemaSql(String schema) {
		return "CREATE SCHEMA IF NOT EXISTS " + schema;
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
	protected String getDropSchemaSql(String schema) {
		return "DROP SCHEMA IF EXISTS " + schema;
	}

	@Override
	protected String getDropDatabaseSql(String database) {
		return "DROP DATABASE IF EXISTS " + database;
	}


}
