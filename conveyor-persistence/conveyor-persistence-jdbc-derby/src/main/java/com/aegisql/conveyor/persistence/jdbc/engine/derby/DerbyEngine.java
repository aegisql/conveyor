package com.aegisql.conveyor.persistence.jdbc.engine.derby;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.init.JdbcScriptSection;

import java.util.UUID;

/**
 * The Class DerbyEngine.
 *
 * @param <K> the key type
 */
public class DerbyEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new derby engine.
	 *
	 * @param keyClass the key class
	 */
	public DerbyEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	@Override
	protected void init() {
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("jdbc:derby:{database};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby:{database};create=true");
		setConnectionUrlTemplate("jdbc:derby:{database};");
	}

	@Override
	protected boolean switchUrlTemplae(String temlate) {
		if ((connectionFactory.getDatabase() == null || connectionFactory.getDatabase().isBlank())
				&& connectionFactory.getSchema() != null
				&& !connectionFactory.getSchema().isBlank()) {
			connectionFactory.setDatabase(connectionFactory.getSchema());
		}
		return super.switchUrlTemplae(temlate);
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
	protected JdbcScriptSection getCreateDatabaseScriptSection(String database) {
		if (database == null || database.isBlank()) {
			return null;
		}
		return JdbcScriptSection.note(
				"Create database",
				"Derby database creation is driven by the JDBC URL create=true flag, not a standalone SQL statement.",
				"Open database " + database + " with create=true before running the remaining statements."
		);
	}

	@Override
	protected JdbcScriptSection getDropDatabaseScriptSection(String database) {
		if (database == null || database.isBlank()) {
			return null;
		}
		return JdbcScriptSection.note(
				"Drop database",
				"Derby database cleanup is environment-specific and is not emitted as SQL.",
				"Remove database " + database + " using your Derby administration procedure if needed."
		);
	}
}
