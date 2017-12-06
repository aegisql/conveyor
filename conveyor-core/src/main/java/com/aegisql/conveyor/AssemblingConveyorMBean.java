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
	public String getExpirationPosponeTimeMsec();
	
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
	
	public <K,L,OUT> Conveyor<K, L, OUT> conveyor();
	
}
