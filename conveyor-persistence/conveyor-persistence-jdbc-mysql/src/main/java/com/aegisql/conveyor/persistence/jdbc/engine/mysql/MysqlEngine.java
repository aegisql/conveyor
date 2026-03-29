package com.aegisql.conveyor.persistence.jdbc.engine.mysql;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.util.UUID;

/**
 * The Class MysqlEngine.
 *
 * @param <K> the key type
 */
public class MysqlEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new mysql engine.
	 *
	 * @param keyClass the key class
	 */
	public MysqlEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	@Override
	public int defaultPort() {
		return 3306;
	}

	@Override
	public String defaultDriverClassName() {
		return "com.mysql.cj.jdbc.Driver";
	}

	@Override
	protected void init() {
		setConnectionUrlTemplateForInitDatabase("jdbc:mysql://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:mysql://{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:mysql://{host}:{port}/{database}");
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
	protected String getCreateDatabaseSql(String database) {
		return "CREATE DATABASE IF NOT EXISTS " + database;
	}

	@Override
	protected String getCreatePartTableSql(String partTable) {
		return super.getCreatePartTableSql(partTable).replaceFirst("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
	}

	@Override
	protected String getCompletedLogTableSql(String completedLogTable) {
		return super.getCompletedLogTableSql(completedLogTable).replaceFirst("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
	}

	@Override
	protected String getDropUniquePartTableIndexSql(String partTable, java.util.List<String> fields) {
		return "DROP INDEX " + indexName(partTable, fields) + " ON " + partTable;
	}

	@Override
	protected String getDropPartTableIndexSql(String partTable) {
		return "DROP INDEX " + indexName(partTable) + " ON " + partTable;
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
	protected String getDropDatabaseSql(String database) {
		return "DROP DATABASE IF EXISTS " + database;
	}

}
