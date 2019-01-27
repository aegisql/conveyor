package com.aegisql.conveyor.persistence.jdbc.builders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class ConnectionSupplier implements Supplier<Connection>, Cloneable {

	private Connection connection;
	
	private final String connectionUrl;
	private final Properties properties;
	
	public ConnectionSupplier(String connectionUrl,Properties properties) {
		this.connectionUrl = connectionUrl;
		this.properties = properties;
	}

	public ConnectionSupplier(String connectionUrl) {
		this(connectionUrl,null);
	}

	@Override
	public Connection get() {
		if(connection == null) {
			try {
				if(properties == null) {
					connection = DriverManager.getConnection(connectionUrl);
				} else {
					connection = DriverManager.getConnection(connectionUrl, properties);
				}
			} catch (SQLException e) {
				throw new PersistenceException("Failed establish connection to "+connectionUrl+(properties==null ? "" : " "+properties), e);
			}
		}
		return connection;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConnectionSupplier [connectionUrl=").append(connectionUrl).append(", properties=")
				.append(properties).append("]");
		return builder.toString();
	}

	public ConnectionSupplier clone() {
		return new ConnectionSupplier(connectionUrl,properties);
	}
	
}
