package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class ShoppingCart<K, V, L> extends AbstractCart<K, V, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public ShoppingCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super(k, v, label, ttl, timeUnit);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public ShoppingCart(K k, V v, L label, long expiration) {
		super(k, v, label, expiration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 */
	public ShoppingCart(K k, V v, L label) {
		super(k, v, label);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param key the key
	 * @param value the value
	 * @param label the label
	 * @param duration the duration
	 */
	public ShoppingCart(K key, V value, L label, Duration duration) {
		super(key,value,label,duration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param key the key
	 * @param value the value
	 * @param label the label
	 * @param instant the instant
	 */
	public ShoppingCart(K key, V value, L label, Instant instant) {
		super(key,value,label,instant);
		Objects.requireNonNull(k);
	}

	/**
	 * Next cart.
	 *
	 * @param <V1> the generic type
	 * @param newValue the new value
	 * @param newLabel the new label
	 * @return the cart
	 */
	public <V1> ShoppingCart<K,V1,L> nextCart(V1 newValue,L newLabel) {
		return new ShoppingCart<>(this.getKey(),newValue,newLabel,this.getExpirationTime());
	}

	/**
	 * Next cart.
	 *
	 * @param <V1> the generic type
	 * @param newValue the new value
	 * @return the cart
	 */
	public <V1> ShoppingCart<K,V1,L> nextCart(V1 newValue) {
		return new ShoppingCart<>(this.getKey(), newValue, this.getLabel(), this.getExpirationTime());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new ShoppingCart<K,V,L>(getKey(), getValue(), getLabel(),getExpirationTime());
	}

	
}
