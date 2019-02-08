package com.aegisql.conveyor.persistence.jdbc;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import com.aegisql.conveyor.persistence.core.Persistence;

// TODO: Auto-generated Javadoc
/**
 * The Interface JdbcPersistenceMBean.
 *
 * @param <K> the key type
 */
public interface JdbcPersistenceMBean<K> extends Supplier<Persistence<K>>{
	
	/** The Constant mBeanServer. */
	final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	/**
	 * Gets the engine type.
	 *
	 * @return the engine type
	 */
	public String getEngineType();
	
	/**
	 * Gets the database.
	 *
	 * @return the database
	 */
	public String getDatabase();
	
	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	public String getSchema();
	
	/**
	 * Gets the part table.
	 *
	 * @return the part table
	 */
	public String getPartTable();
	
	/**
	 * Gets the complete table.
	 *
	 * @return the complete table
	 */
	public String getCompleteTable();
	
	/**
	 * Gets the archive strategy.
	 *
	 * @return the archive strategy
	 */
	public String getArchiveStrategy();
	
	/**
	 * Gets the archive strategy details.
	 *
	 * @return the archive strategy details
	 */
	public String getArchiveStrategyDetails();
	
	/**
	 * Checks if is encrypted.
	 *
	 * @return true, if is encrypted
	 */
	public boolean isEncrypted();
	
	/**
	 * Gets the engine.
	 *
	 * @return the engine
	 */
	public String getEngine();
	
	/**
	 * Gets the host.
	 *
	 * @return the host
	 */
	public String getHost();
	
	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public int getPort();
	
	/**
	 * Min compact size.
	 *
	 * @return the int
	 */
	public int minCompactSize();
	
	/**
	 * Gets the max batch size.
	 *
	 * @return the max batch size
	 */
	public int getMaxBatchSize();
	
	/**
	 * Gets the max batch time.
	 *
	 * @return the max batch time
	 */
	public long getMaxBatchTime();
	
}
