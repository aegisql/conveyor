package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

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
	public MysqlEngine(Class<K> keyClass, ConnectionFactory connectionFactory) {
		super(keyClass,connectionFactory);
	}

	@Override
	protected void init() {
		if(connectionFactory.getPort()==0) connectionFactory.setPort(3306);
		if(connectionFactory.getHost()==null) connectionFactory.setHost("localhost");
		setDriver("com.mysql.cj.jdbc.Driver");
		setConnectionUrlTemplateForInitDatabase("jdbc:mysql://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:mysql://{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:mysql://{host}:{port}/{database}");
	}

}
