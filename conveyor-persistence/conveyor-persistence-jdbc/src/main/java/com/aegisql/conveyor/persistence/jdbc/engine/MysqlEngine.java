package com.aegisql.conveyor.persistence.jdbc.engine;

public class MysqlEngine <K> extends GenericEngine<K> {
	public MysqlEngine(Class<K> keyClass) {
		super(
				keyClass,
				"com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://{host}:{port}/",
				"",
				"jdbc:mysql://{host}:{port}/{database}",
				"jdbc:mysql://{host}:{port}/{database}"
				);
		setPort(3306);
		setHost("localhost");
	}

	@Override
	public void setUser(String user) {
		super.setUser(user);
		if(notEmpty(user)) {
			setProperty("user", user);
		}
	}

	@Override
	public void setPassword(String password) {
		super.setPassword(password);
		if(notEmpty(password)) {
			setProperty("password", password);
		}
	}
	
	
}
