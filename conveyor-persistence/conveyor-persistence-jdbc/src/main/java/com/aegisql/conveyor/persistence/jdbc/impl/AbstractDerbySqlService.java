package com.aegisql.conveyor.persistence.jdbc.impl;

import java.util.Properties;

import com.aegisql.conveyor.persistence.jdbc.SqlService;

public abstract class AbstractDerbySqlService implements SqlService {

	protected final Properties properties;
	protected final static String PROTOCOL = "jdbc:derby:";
	protected final static int PORT = 1527;

	
	public AbstractDerbySqlService(Properties properties) {
		this.properties = properties;
	}

	@Override
	public String getCreatePartTableSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCreatePartTableIndexSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCompletedLogTableSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSaveCartSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSaveCompletedBuildKeySql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPartsByIdsSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExpiredPartsSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAllPartIdsSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAllUnfinishedPartIdsSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAllCompletedKeysSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAllStaticPartSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCountAllActivePartSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return "JdbcPersistence";
	}

}
