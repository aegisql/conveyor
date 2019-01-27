package com.aegisql.conveyor.persistence.jdbc.builders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class ConnectionBuilder implements Supplier<Connection>{

	private String driver;
	private String protocol;
	private String host;
	private int port;
	private String username;
	private String password;

	private String connectionUrl;
	private Properties properties;
	
	public ConnectionBuilder() {
	}

	@Override
	public Connection get() {
		try {
			return DriverManager.getConnection(connectionUrl, properties);
		} catch (SQLException e) {
			throw new PersistenceException("ConnectionBuilder SQL Exception", e);
		}
	}

}
