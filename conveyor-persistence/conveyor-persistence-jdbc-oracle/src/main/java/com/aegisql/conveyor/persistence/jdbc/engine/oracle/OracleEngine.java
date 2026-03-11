package com.aegisql.conveyor.persistence.jdbc.engine.oracle;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.Field;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.SQLException;
import java.util.Locale;

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
		setField(CART_KEY, mapFieldTypeForOracle(fields.get(CART_KEY)));
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
	protected String getCreatePartTableSql(String partTable) {
		StringBuilder sb = new StringBuilder("CREATE TABLE ")
				.append(partTable)
				.append(" (");

		fields.forEach((col, type) -> sb.append(col).append(" ").append(mapFieldTypeForOracle(type)).append(","));
		additionalFields.forEach(f -> sb.append(oracleAdditionalFieldType(f)).append(","));
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String getCompletedLogTableSql(String completedLogTable) {
		return "CREATE TABLE "
				+ completedLogTable + " ("
				+ CART_KEY + " " + mapFieldTypeForOracle(fields.get(CART_KEY)) + " PRIMARY KEY"
				+ ",COMPLETION_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL"
				+ ")";
	}

	private String oracleAdditionalFieldType(Field<?> field) {
		return field.getName() + " " + mapFieldTypeForOracle(getFieldType(field.getFieldClass()));
	}

	private String mapFieldTypeForOracle(String fieldType) {
		if (fieldType == null) {
			return "VARCHAR2(255 CHAR)";
		}
		String mapped = fieldType
				.replaceAll("\\bBIGINT\\b", "NUMBER(19)")
				.replaceAll("\\bSMALLINT\\b", "NUMBER(5)")
				.replaceAll("\\bTINYINT\\b", "NUMBER(3)")
				.replaceAll("\\bINT\\b", "NUMBER(10)")
				.replace("VARCHAR(", "VARCHAR2(");
		if (mapped.contains(" NOT NULL DEFAULT ")) {
			String[] parts = mapped.split(" NOT NULL DEFAULT ", 2);
			return parts[0] + " DEFAULT " + parts[1] + " NOT NULL";
		}
		return mapped;
	}
}
