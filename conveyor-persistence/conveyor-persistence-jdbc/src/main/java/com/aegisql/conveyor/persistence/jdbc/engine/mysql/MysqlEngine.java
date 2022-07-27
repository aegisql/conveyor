package com.aegisql.conveyor.persistence.jdbc.engine.mysql;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
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

}
