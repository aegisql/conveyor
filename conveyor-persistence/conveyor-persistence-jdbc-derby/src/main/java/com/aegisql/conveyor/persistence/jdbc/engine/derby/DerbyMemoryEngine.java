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
public class DerbyMemoryEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new derby engine.
	 *
	 * @param keyClass the key class
	 */
	public DerbyMemoryEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	@Override
	protected void init() {
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("jdbc:derby:memory:{schema};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby:memory:{schema};create=true");
		setConnectionUrlTemplate("jdbc:derby:memory:{schema};");
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
		return JdbcScriptSection.note(
				"Create database",
				"Derby memory databases are created when the memory URL is opened.",
				"No standalone SQL statement is emitted for Derby memory database creation."
		);
	}

	@Override
	protected JdbcScriptSection getDropDatabaseScriptSection(String database) {
		return JdbcScriptSection.note(
				"Drop database",
				"Derby memory databases disappear when the owning process ends.",
				"No standalone SQL drop statement is emitted for Derby memory cleanup."
		);
	}
}
