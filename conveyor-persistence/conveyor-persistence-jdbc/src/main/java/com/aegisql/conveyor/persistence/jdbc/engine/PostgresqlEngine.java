package com.aegisql.conveyor.persistence.jdbc.engine;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Auto-generated Javadoc
/**
 * The Class PostgresqlEngine.
 *
 * @param <K> the key type
 */
public class PostgresqlEngine <K> extends GenericEngine<K> {
	
	/**
	 * Instantiates a new postgresql engine.
	 *
	 * @param keyClass the key class
	 */
	public PostgresqlEngine(Class<K> keyClass) {
		super(
				keyClass,
				"org.postgresql.Driver",
				"jdbc:postgresql://{host}:{port}/",
				"jdbc:postgresql://{host}:{port}/{database}",
				"jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}",
				"jdbc:postgresql://{host}:{port}/{database}?currentSchema={schema}"
				);
		setPort(5432);
		setHost("localhost");
		setField(CART_VALUE, "BYTEA");
		setField(CREATION_TIME, "TIMESTAMP");
		setField(EXPIRATION_TIME, "TIMESTAMP");

	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#databaseExists(java.lang.String)
	 */
	@Override
	public boolean databaseExists(String database) {
		AtomicBoolean res = new AtomicBoolean(false);
		String query = "SELECT 1 FROM pg_catalog.pg_database WHERE lower(datname) = lower(?)";

		connectAndDo(connectionUrlTemplateForInitDatabase, con->{
			try(PreparedStatement st = con.prepareStatement(query)) {
				st.setString(1, database);
				ResultSet rs = st.executeQuery();
				res.set(rs.next());
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		return res.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#partTableIndexExists(java.lang.String)
	 */
	@Override
	public boolean partTableIndexExists(String partTable, String indexName) {
		AtomicBoolean res = new AtomicBoolean(false);
		String query = "SELECT 1 FROM pg_catalog. pg_indexes WHERE lower(indexname) = lower(?)";

		connectAndDo(connectionUrlTemplateForInitTablesAndIndexes, con->{
			try(PreparedStatement st = con.prepareStatement(query)) {
				st.setString(1, indexName);
				ResultSet rs = st.executeQuery();
				res.set(rs.next());
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		});
		return res.get();
	}

	@Override
	public DataSource getDataSource() {
		return new PGSimpleDataSource();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine#getEngineSpecificExpirationTimeRange()
	 */
	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > '1970-01-01 00:00:01' AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
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
