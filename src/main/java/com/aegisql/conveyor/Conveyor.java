/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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
 * @param <OUT> the target class type
 */
public interface Conveyor<K, L, OUT> {

	/**
	 * Adds the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public <V> boolean add(Cart<K,V,L> cart);
	public <V> boolean add(K key, V value, L label);
	public <V> boolean add(K key, V value, L label, long expirationTime);
	public <V> boolean add(K key, V value, L label, long ttl, TimeUnit unit);
	public <V> boolean add(K key, V value, L label, Duration duration);
	public <V> boolean add(K key, V value, L label, Instant instant);

	
	/**
	 * Offers the cart to the input queue.
	 *
	 * @param cart the cart
	 * @return true, if successful
	 */
	public <V> boolean offer(Cart<K,V,L> cart);
	public <V> boolean offer(K key, V value, L label);
	public <V> boolean offer(K key, V value, L label, long expirationTime);
	public <V> boolean offer(K key, V value, L label, long ttl, TimeUnit unit);
	public <V> boolean offer(K key, V value, L label, Duration duration);
	public <V> boolean offer(K key, V value, L label, Instant instant);
	
	/**
	 * Adds the command to the management queue.
	 *
	 * @param command Cart
	 * @return true, if successful
	 */
	public <V> boolean addCommand(AbstractCommand<K, V> command);
	public <V> boolean addCommand(K key, V value, CommandLabel label);
	public <V> boolean addCommand(K key, V value, CommandLabel label, long expirationTime);
	public <V> boolean addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit);
	public <V> boolean addCommand(K key, V value, CommandLabel label, Duration duration);
	public <V> boolean addCommand(K key, V value, CommandLabel label, Instant instant);

}
