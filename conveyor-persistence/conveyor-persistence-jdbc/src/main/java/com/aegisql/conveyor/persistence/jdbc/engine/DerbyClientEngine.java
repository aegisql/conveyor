package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import org.apache.derby.client.BasicClientDataSource;

import javax.sql.DataSource;

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
	public DerbyClientEngine(Class<K> keyClass) {
		super(
				keyClass,
				"org.apache.derby.jdbc.ClientDriver",
				"",
				"jdbc:derby://{host}:{port}/{schema};create=true",
				"jdbc:derby://{host}:{port}/{schema};create=true",
				"jdbc:derby://{host}:{port}/{schema};"
				);
		setHost("localhost");
		setPort(1527);
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
	}

	@Override
	public DataSource getDataSource() {
		return new BasicClientDataSource();
	}
}
