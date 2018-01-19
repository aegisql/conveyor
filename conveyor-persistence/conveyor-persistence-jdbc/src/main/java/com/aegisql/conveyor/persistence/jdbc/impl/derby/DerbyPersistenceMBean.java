package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import com.aegisql.conveyor.persistence.core.Persistence;

public interface DerbyPersistenceMBean<K> extends Supplier<Persistence<K>>{
	
	final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	public String getSchema();
	public String getPartTable();
	public String getCompleteTable();
	public String getArchiveStrategy();
	public String getArchiveStrategyProperties();
	public boolean isEncrypted();
	public String getDriver();
	public String getHost();
	public int getPort();
	public int getMaxBatchSize();
	public long getMaxBatchTime();
	
}
