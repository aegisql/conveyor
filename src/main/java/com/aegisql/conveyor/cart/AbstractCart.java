/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.cart;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

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
	
	/** The created. */
	protected final long created = System.currentTimeMillis();
	
	/** The expiration. */
	protected final long expiration;
	
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
		this.expiration = created + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
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
		this.expiration = 0;
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
		this.expiration = expiration;
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
		return created;
	}
	
	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	public long getExpirationTime() {
		return expiration;
	}
	
	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return expiration > 0 && expiration <= System.currentTimeMillis();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Cart [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", created=" + new Date(created) + 
				(expiration > 0 ? (", expires=" + new Date(expiration) ) : ", unexpireable") +
				 "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		if( this.created < ((AbstractCart<?,?,?>)o).created) {
			return -1;
		}
		if( this.created > ((AbstractCart<?,?,?>)o).created) {
			return +1;
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( this.expiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = this.expiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}
	
}
