package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

/**
 * The Class DerbyClientEngine.
 *
 * @param <K> the key type
 */
public class DerbyClientEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new derby client engine.
	 *
	 * @param keyClass the key class
	 */
	public DerbyClientEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		super(keyClass, connectionFactory);
	}

	@Override
	protected void init() {
		if(connectionFactory.getPort()==0) connectionFactory.setPort(1527);
		if(connectionFactory.getHost()==null) connectionFactory.setHost("localhost");
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setDriver("org.apache.derby.jdbc.ClientDriver");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("jdbc:derby://{host}:{port}/{schema};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby://{host}:{port}/{schema};create=true");
		setConnectionUrlTemplate("jdbc:derby://{host}:{port}/{schema};");
	}
}
