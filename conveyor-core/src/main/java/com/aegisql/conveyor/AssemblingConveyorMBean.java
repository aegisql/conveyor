/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Interface AssemblingConveyorMBean.
 */
public interface AssemblingConveyorMBean {
	
	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public String getStatus();
	
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
	 * Gets the thread id.
	 *
	 * @return the thread id
	 */
	public long getThreadId();
	
	/**
	 * Gets the input queue size.
	 *
	 * @return the input queue size
	 */
	public int getInputQueueSize();
	
	/**
	 * Gets the collector size.
	 *
	 * @return the collector size
	 */
	public int getCollectorSize();
	
	/**
	 * Gets the command queue size.
	 *
	 * @return the command queue size
	 */
	public int getCommandQueueSize();
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning();
	
	/**
	 * Checks if is l balanced.
	 *
	 * @return true, if is l balanced
	 */
	public boolean isLBalanced();
	
	/**
	 * Gets the default builder timeout msec.
	 *
	 * @return the default builder timeout msec
	 */
	public long getDefaultBuilderTimeoutMsec();
	
	/**
	 * Gets the idle heart beat msec.
	 *
	 * @return the idle heart beat msec
	 */
	public long getIdleHeartBeatMsec();
	
	/**
	 * Gets the expiration pospone time msec.
	 *
	 * @return the expiration pospone time msec
	 */
	public long getExpirationPostponeTimeMsec();
	
	/**
	 * Gets the forwarding results to.
	 *
	 * @return the forwarding results to
	 */
	public String getForwardingResultsTo();
	
	/**
	 * Gets the accepted labels.
	 *
	 * @return the accepted labels
	 */
	public String getAcceptedLabels();
	
	/**
	 * Gets the cart counter.
	 *
	 * @return the cart counter
	 */
	public long getCartCounter();
	
	/**
	 * Gets the command counter.
	 *
	 * @return the command counter
	 */
	public long getCommandCounter();
	
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
	 * Interrupt.
	 */
	public void interrupt();
	
	/**
	 * Sets the idle heart beat msec.
	 *
	 * @param msec the new idle heart beat msec
	 */
	void setIdleHeartBeatMsec(long msec);
	
	/**
	 * Sets the default builder timeout msec.
	 *
	 * @param msec the new default builder timeout msec
	 */
	void setDefaultBuilderTimeoutMsec(long msec);
	
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
	void setExpirationPostponeTimeMsec(long msec);
	
	boolean isSuspended();
	
	void suspend();
	
	void resume();
	
}
