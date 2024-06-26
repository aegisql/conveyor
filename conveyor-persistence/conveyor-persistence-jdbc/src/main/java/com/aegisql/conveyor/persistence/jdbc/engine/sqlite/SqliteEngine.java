package com.aegisql.conveyor.persistence.jdbc.engine.sqlite;

// TODO: Auto-generated Javadoc

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.WrappedExternalDataSource;
import org.sqlite.SQLiteConfig;

import java.sql.SQLException;

/**
 * The Class SqliteEngine.
 *
 * @param <K> the key type
 */
public class SqliteEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new sqlite engine.
	 *
	 * @param keyClass the key class
	 */
	public SqliteEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > 0 AND EXPIRATION_TIME < strftime('%s','now')*1000";
	}

	@Override
	public String defaultDriverClassName() {
		return "org.sqlite.JDBC";
	}

	@Override
	public ConnectionFactory<?> defaultConnectionFactory() {
		SQLiteConfig config = new SQLiteConfig();
		//config.setJournalMode(SQLiteConfig.JournalMode.PERSIST);
		config.setJournalMode(SQLiteConfig.JournalMode.WAL);
		config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);

		ConnectionFactory cf = ConnectionFactory.cachingFactoryInstance(f-> new WrappedExternalDataSource(()-> {
            try {
                return config.createConnection(f.getUrl());
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }));
		return cf;
	}

	@Override
	protected void init() {
		setConnectionUrlTemplateForInitDatabase("");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:sqlite:{database}");
		setConnectionUrlTemplate("jdbc:sqlite:{database}");
	}

}
