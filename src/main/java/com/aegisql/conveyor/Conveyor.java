/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Interface Conveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <IN> the generic type
 * @param <OUT> the generic type
 */
public interface Conveyor<K, L, IN extends Cart<K,?,L>,OUT> {

	/**
	 * Adds the.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean add(IN cart);
	
	/**
	 * Offer.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean offer(IN cart);
	
	/**
	 * Adds the command.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean addCommand(Cart<K,?,Command> cart);

}
