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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#setUser(java.lang.String)
	 */
	@Override
	public void setUser(String user) {
		super.setUser(user);
		if(notEmpty(user)) {
			setProperty("user", user);
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#setPassword(java.lang.String)
	 */
	@Override
	public void setPassword(String password) {
		super.setPassword(password);
		if(notEmpty(password)) {
			setProperty("password", password);
		}
	}

	@Override
	protected void init() {
		setPort(3306);
		setHost("localhost");
		setDriver("com.mysql.cj.jdbc.Driver");
		setConnectionUrlTemplateForInitDatabase("jdbc:mysql://{host}:{port}/");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:mysql://{host}:{port}/{database}");
		setConnectionUrlTemplate("jdbc:mysql://{host}:{port}/{database}");
	}

}
