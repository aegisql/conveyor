package com.aegisql.conveyor.persistence.jdbc;

public interface SqlService {
	public String getDriver();
	public String getConnectionUrl();
	public String getCreatePartTableSql();
	public String getCreatePartTableIndexSql();
	public String getCompletedLogTableSql();
	public String getSaveCartSql();
	public String getSaveCompletedBuildKeySql();
	public String getPartsByIdsSql();
	public String getExpiredPartsSql();
	public String getAllPartIdsSql();
	public String getAllUnfinishedPartIdsSql();
	public String getAllCompletedKeysSql();
	public String getAllStaticPartSql();
	public String getCountAllActivePartSql();

}
