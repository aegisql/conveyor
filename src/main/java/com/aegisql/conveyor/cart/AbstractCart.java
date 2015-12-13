/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.cart;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.DelayHolder;

// TODO: Auto-generated Javadoc
/**
 * The Class Cart.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public abstract class AbstractCart<K,V,L> implements Cart<K,V,L> {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5414733837801886611L;

	/** The k. */
	protected final K k;
	
	/** The v. */
	protected final V v;
	
	/** The label. */
	protected final L label;
	
	protected final DelayHolder delay;
	
	/**
	 * Instantiates a new cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public AbstractCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		Objects.requireNonNull(k);
		this.k          = k;
		this.v          = v;
		this.label      = label;
		delay = new DelayHolder(ttl,timeUnit);
	}
	
	/**
	 * Instantiates a new cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 */
	public AbstractCart(K k, V v, L label) {
		Objects.requireNonNull(k);
		this.k = k;
		this.v = v;
		this.label = label;
		delay = new DelayHolder();
	}

	/**
	 * Instantiates a new cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public AbstractCart(K k, V v, L label, long expiration) {
		Objects.requireNonNull(k);
		this.k = k;
		this.v = v;
		this.label = label;
		delay = new DelayHolder(expiration);
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey() {
		return k;
	}
	
	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public V getValue() {
		return v;
	}
	
	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public L getLabel() {
		return label;
	}
	
	/**
	 * Gets the creation time.
	 *
	 * @return the creation time
	 */
	public long getCreationTime() {
		return delay.getCreatedTime();
	}
	
	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	public long getExpirationTime() {
		return delay.getExpirationTime();
	}
	
	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return delay.expired();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Cart [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", delay=" + delay +
				 "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		return delay.compareTo(((AbstractCart<?,?,?>)o).delay);
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return delay.getDelay(unit);
	}
	
}
