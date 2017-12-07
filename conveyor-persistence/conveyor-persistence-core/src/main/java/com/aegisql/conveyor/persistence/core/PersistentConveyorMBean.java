package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.Conveyor;

public interface PersistentConveyorMBean {
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName();
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType();
	
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning();

	/**
	 * Conveyor.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @return the conveyor
	 */
	public <K,L,OUT> Conveyor<K, L, OUT> conveyor();

}
