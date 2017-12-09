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
	
	/**
	 * Stop.
	 */
	public void stop();
	
	/**
	 * Complete and stop.
	 */
	public void completeAndStop();
	
	/**
	 * Sets the idle heart beat msec.
	 *
	 * @param msec the new idle heart beat msec
	 */
	void idleHeartBeatMsec(long msec);
	
	/**
	 * Sets the default builder timeout msec.
	 *
	 * @param msec the new default builder timeout msec
	 */
	void defaultBuilderTimeoutMsec(long msec);
	
	/**
	 * Reject unexpireable carts older than msec.
	 *
	 * @param msec the msec
	 */
	void rejectUnexpireableCartsOlderThanMsec(long msec);
	
	/**
	 * Sets the expiration postpone time msec.
	 *
	 * @param msec the new expiration postpone time msec
	 */
	void expirationPostponeTimeMsec(long msec);

	
}
