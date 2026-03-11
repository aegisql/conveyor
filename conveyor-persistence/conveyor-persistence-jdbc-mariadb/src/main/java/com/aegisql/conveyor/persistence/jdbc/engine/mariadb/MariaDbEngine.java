package com.aegisql.conveyor.persistence.jdbc.engine.mariadb;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
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
	public MariaDbEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	public int defaultPort() {
		return 3306;
	}

	@Override
	public String defaultDriverClassName() {
		return "org.mariadb.jdbc.Driver";
	}
	@Override
	protected void init() {
		setConnectionUrlTemplateForInitDatabase("jdbc:mariadb://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:mariadb://{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:mariadb://{host}:{port}/{database}");
	}

}
