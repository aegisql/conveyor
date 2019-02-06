package com.aegisql.conveyor.persistence.jdbc.engine;

public class DerbyClientEngine <K> extends GenericEngine<K> {
	
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
}
