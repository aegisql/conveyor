package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc
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
	public MariaDbEngine(Class<K> keyClass) {
		super(
				keyClass,
				"org.mariadb.jdbc.Driver",
				"jdbc:mariadb://{host}:{port}/",
				"",
				"jdbc:mariadb://{host}:{port}/{database}",
				"jdbc:mariadb://{host}:{port}/{database}"
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
	
	
}
