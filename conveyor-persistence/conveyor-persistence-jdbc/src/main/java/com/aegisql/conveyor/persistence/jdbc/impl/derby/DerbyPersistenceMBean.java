package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

public interface DerbyPersistenceMBean {
	
	final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	public String getSchema();
	public String getPartTable();
	public String getCompleteTable();
	public String getArchiveStrategy();
	public boolean isEncrypted();
	public String getDriver();
	public String getHost();
	public int getPort();
	public int getMaxBatchSize();
	public long getMaxBatchTime();
	
}
