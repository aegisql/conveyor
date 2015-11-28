/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.command.AbstractCommand;

// TODO: Auto-generated Javadoc
/**
 * The Interface Conveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <IN> the data cart type
 * @param <OUT> the target class type
 */
public interface Conveyor<K, L, IN extends Cart<K,?,L>,OUT> {

	/**
	 * Adds the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean add(IN cart);
	
	/**
	 * Offers the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean offer(IN cart);
	
	/**
	 * Adds the command to the management queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public boolean addCommand(AbstractCommand<K, ?> command);

}
