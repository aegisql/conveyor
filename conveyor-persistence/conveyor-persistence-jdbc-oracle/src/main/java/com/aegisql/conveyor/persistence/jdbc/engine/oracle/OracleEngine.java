package com.aegisql.conveyor.persistence.jdbc.engine.oracle;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

public class OracleEngine<K> extends GenericEngine<K> {

	public OracleEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	@Override
	public boolean databaseExists(String database) {
		if (database == null || database.isBlank()) {
			return true;
		}
		switchUrlTemplae(connectionUrlTemplateForInitDatabase);
		String actualService = getScalarValue(
				"SELECT SYS_CONTEXT('USERENV', 'SERVICE_NAME') FROM DUAL",
				st -> {},
				rs -> {
					try {
						return rs.getString(1);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				}
		);
		if (actualService == null) {
			return false;
		}
		return actualService.toUpperCase(Locale.ROOT).startsWith(database.toUpperCase(Locale.ROOT));
	}

	@Override
	public boolean schemaExists(String schema) {
		if (schema == null || schema.isBlank()) {
			return true;
		}
		switchUrlTemplae(connectionUrlTemplateForInitSchema);
		Integer exists = getScalarValue(
				"SELECT 1 FROM ALL_USERS WHERE USERNAME = UPPER(?)",
				st -> {
					try {
						st.setString(1, schema);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				},
				rs -> {
					try {
						return rs.getInt(1);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				}
		);
		return exists != null && exists == 1;
	}

	@Override
	public boolean partTableExists(String partTable) {
		switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes);
		Integer exists = getScalarValue(
				"SELECT 1 FROM USER_TABLES WHERE TABLE_NAME = UPPER(?)",
				st -> {
					try {
						st.setString(1, partTable);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				},
				rs -> {
					try {
						return rs.getInt(1);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				}
		);
		return exists != null && exists == 1;
	}

	@Override
	public boolean completedLogTableExists(String completedLogTable) {
		return partTableExists(completedLogTable);
	}

	@Override
	public boolean partTableIndexExists(String partTable, String indexName) {
		switchUrlTemplae(connectionUrlTemplateForInitTablesAndIndexes);
		Integer exists = getScalarValue(
				"SELECT 1 FROM USER_INDEXES WHERE INDEX_NAME = UPPER(?)",
				st -> {
					try {
						st.setString(1, indexName);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				},
				rs -> {
					try {
						return rs.getInt(1);
					} catch (SQLException e) {
						throw new PersistenceException(e);
					}
				}
		);
		return exists != null && exists == 1;
	}

	@Override
	public void createDatabase(String database) {
		if (databaseExists(database)) {
			return;
		}
		throw new PersistenceException("Oracle service/database creation is not supported by OracleEngine. Use an existing service.");
	}

	@Override
	public void createSchema(String schema) {
		if (schemaExists(schema)) {
			return;
		}
		throw new PersistenceException("Oracle schema/user creation is not supported by OracleEngine. Use an existing user schema.");
	}

	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > TIMESTAMP '1970-01-01 00:00:01' AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}

	@Override
	public int defaultPort() {
		return 1521;
	}

	@Override
	public String defaultDriverClassName() {
		return "oracle.jdbc.OracleDriver";
	}

	@Override
	protected void init() {
		setField(ID, "NUMBER(19) PRIMARY KEY");
		setField(CART_KEY, getKeyFieldType());
		setField(LOAD_TYPE, "VARCHAR2(15 CHAR)");
		setField(CART_LABEL, "VARCHAR2(100 CHAR)");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setField(PRIORITY, "NUMBER(19) DEFAULT 0 NOT NULL");
		setField(CART_VALUE, "BLOB");
		setField(VALUE_TYPE, "VARCHAR2(255 CHAR)");
		setField(CART_PROPERTIES, "CLOB");
		setField(ARCHIVED, "NUMBER(1) DEFAULT 0 NOT NULL");
		setConnectionUrlTemplateForInitDatabase("jdbc:oracle:thin:@//{host}:{port}/{database}");
		setConnectionUrlTemplateForInitSchema("jdbc:oracle:thin:@//{host}:{port}/{database}");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:oracle:thin:@//{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:oracle:thin:@//{host}:{port}/{database}");
	}

	@Override
	protected String getCompletedLogTableSql(String completedLogTable) {
		return "CREATE TABLE "
				+ completedLogTable + " ("
				+ CART_KEY + " " + fields.get(CART_KEY) + " PRIMARY KEY"
				+ ",COMPLETION_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL"
				+ ")";
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
			case "java.lang.Integer" -> "NUMBER(10) NOT NULL";
			case "java.lang.Long" -> "NUMBER(19) NOT NULL";
			case "java.util.UUID" -> "CHAR(36) NOT NULL";
			default -> "VARCHAR2(255 CHAR) NOT NULL";
		};
	}

	private String getKeyFieldType() {
		if(keyClass.isEnum()) {
			int maxLength = 0;
			for(Object constant : keyClass.getEnumConstants()) {
				maxLength = Math.max(maxLength, constant.toString().length());
			}
			return "CHAR(" + maxLength + ")";
		}
		return switch (keyClass.getName()) {
			case "java.lang.Integer" -> "NUMBER(10)";
			case "java.lang.Long" -> "NUMBER(19)";
			case "java.util.UUID" -> "CHAR(36)";
			default -> "VARCHAR2(255 CHAR)";
		};
	}
}
