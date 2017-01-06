package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Map.Entry;
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
public class MultiKeyCart<K, V, L> extends AbstractCart<K, V, L> implements Predicate<Entry<K,?>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	protected Predicate<Entry<K,?>> filter = entry->true; //pass all by default
	
	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public MultiKeyCart(V v, L label, long ttl, TimeUnit timeUnit) {
		super(null, v, label, ttl, timeUnit);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public MultiKeyCart(V v, L label, long expiration) {
		super(null, v, label, expiration);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 */
	public MultiKeyCart(V v, L label) {
		super(null, v, label);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param key the key
	 * @param value the value
	 * @param label the label
	 * @param duration the duration
	 */
	public MultiKeyCart(V value, L label, Duration duration) {
		super(null,value,label,duration);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param key the key
	 * @param value the value
	 * @param label the label
	 * @param instant the instant
	 */
	public MultiKeyCart(V value, L label, Instant instant) {
		super(null,value,label,instant);
	}

	/**
	 * Next cart.
	 *
	 * @param <V1> the generic type
	 * @param newValue the new value
	 * @param newLabel the new label
	 * @return the cart
	 */
	public <V1> MultiKeyCart<K,V1,L> nextCart(V1 newValue,L newLabel) {
		return new MultiKeyCart<>(newValue,newLabel,this.getExpirationTime());
	}

	/**
	 * Next cart.
	 *
	 * @param <V1> the generic type
	 * @param newValue the new value
	 * @return the cart
	 */
	public <V1> MultiKeyCart<K,V1,L> nextCart(V1 newValue) {
		return new MultiKeyCart<>(newValue, this.getLabel(), this.getExpirationTime());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new MultiKeyCart<K,V,L>(getValue(), getLabel(),getExpirationTime());
	}

	@Override
	public boolean test(Entry<K,?> entry) {
		return filter.test(entry);
	}

	
}
