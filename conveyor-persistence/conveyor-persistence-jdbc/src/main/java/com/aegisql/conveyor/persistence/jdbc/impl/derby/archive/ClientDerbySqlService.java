package com.aegisql.conveyor.persistence.jdbc.impl.derby.archive;

import java.util.Properties;

import com.aegisql.conveyor.persistence.jdbc.impl.AbstractDerbySqlService;

public class ClientDerbySqlService extends AbstractDerbySqlService {
	
	private final static String URL_PATTERN   = PROTOCOL+"//{host}:{port}/{schema}";//;user=judy;password=no12see";

	public ClientDerbySqlService(Properties properties) {
		super(properties);
	}

	@Override
	public String getDriver() {
		return "org.apache.derby.jdbc.ClientDriver";
	}

	@Override
	public String getConnectionUrl() {
		return null;
	}

}
