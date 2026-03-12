package com.aegisql.conveyor.persistence.jdbc.engine.sqlserver;

import com.aegisql.conveyor.persistence.jdbc.engine.GenericEngine;
import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Function;

public class SqlServerEngine<K> extends GenericEngine<K> {

	private static final Timestamp NON_EXPIRED_LOWER_BOUND = new Timestamp(1000L);

	public SqlServerEngine(Class<K> keyClass, ConnectionFactory connectionFactory, boolean poolConnection) {
		super(keyClass, connectionFactory, poolConnection);
	}

	@Override
	public int defaultPort() {
		return 1433;
	}

	@Override
	public String defaultDriverClassName() {
		return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	}

	@Override
	protected String getCompletedLogTableSql(String completedLogTable) {
		return "CREATE TABLE "
				+ completedLogTable + " ("
				+ CART_KEY + " " + fields.get(CART_KEY) + " PRIMARY KEY"
				+ ",COMPLETION_TIME DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP"
				+ ")";
	}

	@Override
	protected String getEngineSpecificExpirationTimeRange() {
		return "EXPIRATION_TIME > '1970-01-01T00:00:01' AND EXPIRATION_TIME < CURRENT_TIMESTAMP";
	}

	@Override
	public void buildPartTableQueries(String partTable) {
		super.buildPartTableQueries(partTable);
		this.deleteExpiredPartsSql = "DELETE FROM " + partTable + " WHERE EXPIRATION_TIME > ? AND EXPIRATION_TIME < ?";
		this.updateExpiredPartsSql = "UPDATE " + partTable + " SET ARCHIVED = 1 WHERE EXPIRATION_TIME > ? AND EXPIRATION_TIME < ?";
		this.getExpiredPartQuery = "SELECT "
				+ CART_KEY
				+ "," + CART_VALUE
				+ "," + CART_LABEL
				+ "," + CREATION_TIME
				+ "," + EXPIRATION_TIME
				+ "," + LOAD_TYPE
				+ "," + CART_PROPERTIES
				+ "," + VALUE_TYPE
				+ "," + PRIORITY
				+ " FROM " + partTable
				+ " WHERE EXPIRATION_TIME > ? AND EXPIRATION_TIME < ?";
	}

	@Override
	public void deleteExpiredParts() {
		executePrepared(deleteExpiredPartsSql, this::bindExpirationWindow);
	}

	@Override
	public void updateExpiredParts() {
		executePrepared(updateExpiredPartsSql, this::bindExpirationWindow);
	}

	@Override
	public <T> java.util.List<T> getExpiredParts(Function<java.sql.ResultSet, T> transformer) {
		return getCollectionOfValues(getExpiredPartQuery, this::bindExpirationWindow, transformer);
	}

	private void bindExpirationWindow(PreparedStatement statement) {
		try {
			statement.setTimestamp(1, NON_EXPIRED_LOWER_BOUND);
			statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
		} catch (SQLException e) {
			throw new com.aegisql.conveyor.persistence.core.PersistenceException(e);
		}
	}

	@Override
	protected void init() {
		setField(CART_VALUE, "VARBINARY(MAX)");
		setField(CART_PROPERTIES, "VARCHAR(MAX)");
		setField(CREATION_TIME, "DATETIME2 NOT NULL");
		setField(EXPIRATION_TIME, "DATETIME2 NOT NULL");
		setConnectionUrlTemplateForInitDatabase("jdbc:sqlserver://{host}:{port};encrypt=false;trustServerCertificate=true");
		setConnectionUrlTemplateForInitSchema("");
		setConnectionUrlTemplateForInitTablesAndIndexes("jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=false;trustServerCertificate=true");
		setConnectionUrlTemplate("jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=false;trustServerCertificate=true");
	}
}
