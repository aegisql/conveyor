package com.aegisql.conveyor.persistence.jdbc.impl.derby;

public interface DerbyPersistenceMBean {
	public String getSchema();
	public String getPartTable();
	public String getCompleteTable();
	public String getArchiveStrategy();
}
