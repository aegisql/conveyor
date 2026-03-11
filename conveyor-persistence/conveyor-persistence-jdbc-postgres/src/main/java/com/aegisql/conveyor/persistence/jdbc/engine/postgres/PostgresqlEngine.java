package com.aegisql.conveyor.persistence.jdbc.engine.postgres;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.SQLException;

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


}
