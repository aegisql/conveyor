package com.aegisql.conveyor.persistence.jdbc.builders;

import java.util.LinkedHashMap;
import java.util.UUID;

public class GenericInitPersistenceSql <K> {
	
	
	protected String keyType;
	protected String database;
	protected String schema = "conveyor_db";
	protected String partTable = "PART";
	protected String completedLogTable = "COMPLETED_LOG";

	protected String createPartTableSql;
	protected String completedLogTableSql;
	protected String createPartTableIndexSql;

	protected int loadTypeMaxLength = 15;
	protected int labelMaxLength = 100;
	protected int valueClassMaxLength = 255;
	
	protected LinkedHashMap<String, String> partColumns = new LinkedHashMap<>();

	public GenericInitPersistenceSql(Class<K> keyClass) {
		if(keyClass == Integer.class) {
			this.keyType = "INT";
		} else if(keyClass == Long.class) {
			this.keyType = "BIGINT";
		} else if(keyClass == UUID.class) {
			this.keyType = "CHAR(36)";
		} else if(keyClass.isEnum()) {
			int maxLength = 0;
			for(Object o:keyClass.getEnumConstants()) {
				maxLength = Math.max(maxLength, o.toString().length());
			}
			this.keyType = "CHAR("+maxLength+")";
		} else {
			this.keyType = "VARCHAR(255)";
		}
		
		partColumns.put("ID", "BIGINT PRIMARY KEY");
		partColumns.put("LOAD_TYPE", "CHAR("+loadTypeMaxLength+")");
		partColumns.put("CART_KEY",keyType);
		partColumns.put("CART_LABEL", "VARCHAR("+labelMaxLength +")");
		partColumns.put("CREATION_TIME", "TIMESTAMP NOT NULL");
		partColumns.put("EXPIRATION_TIME", "TIMESTAMP NOT NULL");
		partColumns.put("PRIORITY", "BIGINT NOT NULL DEFAULT 0");
		partColumns.put("CART_VALUE", "BLOB");
		partColumns.put("VALUE_TYPE", "VARCHAR("+valueClassMaxLength+")");
		partColumns.put("CART_PROPERTIES", "TEXT");
		partColumns.put("ARCHIVED", "SMALLINT NOT NULL DEFAULT 0");
		
	}

	public String createPartTableSql() {
		
		if(createPartTableSql == null) {
			StringBuilder sb = new StringBuilder("CREATE TABLE ")
					.append(partTable)
					.append(" (");
			
			partColumns.forEach((col,type)->{
				sb.append(col).append(" ").append(type).append(",");
			});
			sb.deleteCharAt(sb.lastIndexOf(","));
			sb.append(")");
			
			createPartTableSql = sb.toString();
		}
		
		return createPartTableSql;
	}

	public String createCompletedLogTableSql() {
		if(completedLogTableSql==null) {
			completedLogTableSql = "CREATE TABLE "
					+completedLogTable+" ("
					+"CART_KEY "+keyType+" PRIMARY KEY"
					+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
					+")";
		}
		return completedLogTableSql;
	}

	public String createPartTableIndexSql() {
		if(createPartTableIndexSql==null) {
			createPartTableIndexSql = "CREATE INDEX "+partTable+"_IDX ON "+partTable+"(CART_KEY)";
		}
		return createPartTableIndexSql;
	}

	public String createDatabaseSql() {
		if(database != null) {
			return "CREATE DATABASE "+database;
		} else {
			return null;
		}
	}

	public String createSchemaSql() {
		if(schema != null) {
			return "CREATE SCHEMA IF NOT EXISTS "+schema;
		} else {
			return null;
		}
	}

	public void setCompletedLogTable(String completedLogTable) {
		this.completedLogTable = completedLogTable;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setPartTable(String partTable) {
		this.partTable = partTable;
	}

	public void setLoadTypeMaxLength(int loadTypeMaxLength) {
		this.loadTypeMaxLength = loadTypeMaxLength;
	}

	public void setLabelMaxLength(int labelMaxLength) {
		this.labelMaxLength = labelMaxLength;
	}

	public void setValueClassMaxLength(int valueClassMaxLength) {
		this.valueClassMaxLength = valueClassMaxLength;
	}

	public void setPartColumnType(String colName, String colSqlType) {
		partColumns.put(colName, colSqlType);
	}
}
