package com.aegisql.conveyor.persistence.jdbc.engine;

public class DerbyEngine <K> extends GenericEngine<K> {
	
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
