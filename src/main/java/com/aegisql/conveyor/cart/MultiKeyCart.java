package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class MultiKeyCart<K, V, L> extends AbstractCart<K, V, L> implements Predicate<K> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	protected final Predicate<K> filter;
	
	/**
	 * Instantiates a new multikey cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public MultiKeyCart(V v, L label, long ttl, TimeUnit timeUnit) {
		super(null, v, label, ttl, timeUnit);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new multikey cart.
	 *
	 * @param filter K key filtering predicate
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public MultiKeyCart(Predicate<K> filter, V v, L label, long ttl, TimeUnit timeUnit) {
		super(null, v, label, ttl, timeUnit);
		this.filter = filter;
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public MultiKeyCart(V v, L label, long expiration) {
		super(null, v, label, expiration);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param filter K key filtering predicate
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public MultiKeyCart(Predicate<K> filter, V v, L label, long expiration) {
		super(null, v, label, expiration);
		this.filter = filter;
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 */
	public MultiKeyCart(V v, L label) {
		super(null, v, label);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param filter K key filtering predicate
	 * @param v the v
	 * @param label the label
	 */
	public MultiKeyCart(Predicate<K> filter, V v, L label) {
		super(null, v, label);
		this.filter = filter;
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param value the value
	 * @param label the label
	 * @param duration the duration
	 */
	public MultiKeyCart(V value, L label, Duration duration) {
		super(null,value,label,duration);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param filter K key filtering predicate
	 * @param value the value
	 * @param label the label
	 * @param duration the duration
	 */
	public MultiKeyCart(Predicate<K> filter, V value, L label, Duration duration) {
		super(null,value,label,duration);
		this.filter = filter;
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param value the value
	 * @param label the label
	 * @param instant the instant
	 */
	public MultiKeyCart(V value, L label, Instant instant) {
		super(null,value,label,instant);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param filter K key filtering predicate
	 * @param value the value
	 * @param label the label
	 * @param instant the instant
	 */
	public MultiKeyCart(Predicate<K> filter, V value, L label, Instant instant) {
		super(null,value,label,instant);
		this.filter = filter;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new MultiKeyCart<K,V,L>(getValue(), getLabel(),getExpirationTime());
	}

	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		return new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
	}
	
	@Override
	public boolean test(K key) {
		return filter.test(key);
	}
	
}
