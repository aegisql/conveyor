package com.aegisql.conveyor.persistence.jdbc.engine.derby;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

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
	public DerbyClientEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	public int defaultPort() {
		return 1527;
	}

	@Override
	public String defaultDriverClassName() {
		return "org.apache.derby.jdbc.ClientDriver";
	}

	@Override
	protected void init() {
		setField(CART_PROPERTIES, "CLOB");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("jdbc:derby://{host}:{port}/{database};create=true");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:derby://{host}:{port}/{database};create=true");
		setConnectionUrlTemplate("jdbc:derby://{host}:{port}/{database};");
	}

	@Override
	protected boolean switchUrlTemplae(String temlate) {
		if ((connectionFactory.getDatabase() == null || connectionFactory.getDatabase().isBlank())
				&& connectionFactory.getSchema() != null
				&& !connectionFactory.getSchema().isBlank()) {
			connectionFactory.setDatabase(connectionFactory.getSchema());
		}
		return super.switchUrlTemplae(temlate);
	}
}
