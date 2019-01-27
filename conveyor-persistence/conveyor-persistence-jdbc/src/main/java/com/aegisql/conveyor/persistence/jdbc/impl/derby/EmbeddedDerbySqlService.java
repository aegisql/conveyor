package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.util.Properties;

import com.aegisql.conveyor.persistence.jdbc.impl.AbstractDerbySqlService;

public class EmbeddedDerbySqlService extends AbstractDerbySqlService {
	
	private final static String URL_PATTERN = AbstractDerbySqlService.PROTOCOL+"{schema}";

	public EmbeddedDerbySqlService(Properties properties) {
		super(properties);
	}

	@Override
	public String getDriver() {
		return "org.apache.derby.jdbc.EmbeddedDriver";
	}

	@Override
	public String getConnectionUrl() {
		// TODO Auto-generated method stub
		return null;
	}

}
