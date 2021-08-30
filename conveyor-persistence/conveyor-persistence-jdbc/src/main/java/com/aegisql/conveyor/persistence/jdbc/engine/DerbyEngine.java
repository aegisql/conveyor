package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

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
	public DerbyEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		super(keyClass, connectionFactory);
	}

	@Override
	protected void init() {
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setDriver("");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("jdbc:derby:{schema};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby:{schema};create=true");
		setConnectionUrlTemplate("jdbc:derby:{schema};");
	}
}
