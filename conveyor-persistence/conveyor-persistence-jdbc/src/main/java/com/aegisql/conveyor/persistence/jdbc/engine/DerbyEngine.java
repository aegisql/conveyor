package com.aegisql.conveyor.persistence.jdbc.engine;

// TODO: Auto-generated Javadoc
/**
 * The Class DerbyEngine.
 *
 * @param <K> the key type
 */
public class DerbyEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new derby engine.
	 *
	 * @param keyClass the key class
	 */
	public DerbyEngine(Class<K> keyClass) {
		super(
				keyClass,
				"org.apache.derby.jdbc.EmbeddedDriver",
				"",
				"jdbc:derby:{schema};create=true",
				"jdbc:derby:{schema};create=true",
				"jdbc:derby:{schema};"
				);
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
	}
}
