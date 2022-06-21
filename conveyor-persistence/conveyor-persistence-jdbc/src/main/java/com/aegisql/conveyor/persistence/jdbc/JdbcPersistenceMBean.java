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
	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	/**
	 * Gets the engine type.
	 *
	 * @return the engine type
	 */
	String getEngineType();
	
	/**
	 * Gets the database.
	 *
	 * @return the database
	 */
	String getDatabase();
	
	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	String getSchema();
	
	/**
	 * Gets the part table.
	 *
	 * @return the part table
	 */
	String getPartTable();
	
	/**
	 * Gets the complete table.
	 *
	 * @return the complete table
	 */
	String getCompleteTable();
	
	/**
	 * Gets the archive strategy.
	 *
	 * @return the archive strategy
	 */
	String getArchiveStrategy();
	
	/**
	 * Gets the archive strategy details.
	 *
	 * @return the archive strategy details
	 */
	String getArchiveStrategyDetails();
	
	/**
	 * Checks if is encrypted.
	 *
	 * @return true, if is encrypted
	 */
	boolean isEncrypted();
	
	/**
	 * Gets the engine.
	 *
	 * @return the engine
	 */
	String getEngine();
	
	/**
	 * Gets the host.
	 *
	 * @return the host
	 */
	String getHost();
	
	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	int getPort();
	
	/**
	 * Min compact size.
	 *
	 * @return the int
	 */
	int minCompactSize();
	
	/**
	 * Gets the max batch size.
	 *
	 * @return the max batch size
	 */
	int getMaxBatchSize();
	
	/**
	 * Gets the max batch time.
	 *
	 * @return the max batch time
	 */
	long getMaxBatchTime();
	
}
