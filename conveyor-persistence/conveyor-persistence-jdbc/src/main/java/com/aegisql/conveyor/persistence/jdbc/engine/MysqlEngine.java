package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;

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
	public MysqlEngine(Class<K> keyClass) {
		super(
				keyClass,
				"com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://{host}:{port}/",
				"",
				"jdbc:mysql://{host}:{port}/{database}",
				"jdbc:mysql://{host}:{port}/{database}"
				);
		setPort(3306);
		setHost("localhost");
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
	public DataSource getDataSource() {
		return new MysqlDataSource();
	}
}
