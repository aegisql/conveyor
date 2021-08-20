package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc

import org.apache.derby.client.BasicClientDataSource;

import javax.sql.DataSource;

/**
 * The Class DerbyEngine.
 *
 * @param <K> the key type
 */
public class DerbyMemoryEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new derby engine.
	 *
	 * @param keyClass the key class
	 */
	public DerbyMemoryEngine(Class<K> keyClass) {
		super(
				keyClass,
				"",
				"",
				"jdbc:derby:memory:{schema};create=true",
				"jdbc:derby:memory:{schema};create=true",
				"jdbc:derby:memory:{schema};"
				);
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
	}

	@Override
	public DataSource getDataSource() {
		return new BasicClientDataSource();
	}
}
