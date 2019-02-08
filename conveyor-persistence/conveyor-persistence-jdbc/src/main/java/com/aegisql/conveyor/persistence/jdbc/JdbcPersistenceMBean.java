package com.aegisql.conveyor.persistence.jdbc;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import com.aegisql.conveyor.persistence.core.Persistence;

public interface JdbcPersistenceMBean<K> extends Supplier<Persistence<K>>{
	
	final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	public String getEngineType();
	public String getDatabase();
	public String getSchema();
	public String getPartTable();
	public String getCompleteTable();
	public String getArchiveStrategy();
	public String getArchiveStrategyDetails();
	public boolean isEncrypted();
	public String getEngine();
	public String getHost();
	public int getPort();
	public int minCompactSize();
	public int getMaxBatchSize();
	public long getMaxBatchTime();
	
}
