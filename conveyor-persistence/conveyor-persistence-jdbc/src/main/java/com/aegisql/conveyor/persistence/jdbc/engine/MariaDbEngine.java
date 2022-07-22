package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

/**
 * The Class MariaDbEngine.
 *
 * @param <K> the key type
 */
public class MariaDbEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new mysql engine.
	 *
	 * @param keyClass the key class
	 */
	public MariaDbEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		super(keyClass,connectionFactory);
	}


	@Override
	protected void init() {
		if(connectionFactory.getPort()==0) connectionFactory.setPort(3306);
		if(connectionFactory.getHost()==null) connectionFactory.setHost("localhost");
		setDriver("org.mariadb.jdbc.Driver");
		setConnectionUrlTemplateForInitDatabase("jdbc:mariadb://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:mariadb://{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:mariadb://{host}:{port}/{database}");
	}

}
