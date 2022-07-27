package com.aegisql.conveyor.persistence.jdbc.engine.derby;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

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
		setConnectionUrlTemplateForInitSchema("jdbc:derby:{schema};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby:{schema};create=true");
		setConnectionUrlTemplate("jdbc:derby:{schema};");
	}
}
