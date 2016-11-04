package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ShoppingCart<K, V, L> extends AbstractCart<K, V, L> {

	private static final long serialVersionUID = 4055225191822888396L;

	public ShoppingCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super(k, v, label, ttl, timeUnit);
	}

	public ShoppingCart(K k, V v, L label, long expiration) {
		super(k, v, label, expiration);
	}

	public ShoppingCart(K k, V v, L label) {
		super(k, v, label);
	}

	public ShoppingCart(K key, V value, L label, Duration duration) {
		super(key,value,label,duration);
	}

	public ShoppingCart(K key, V value, L label, Instant instant) {
		super(key,value,label,instant);
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

	@Override
	public Cart <K,V,L> copy() {
		return new ShoppingCart<K,V,L>(getKey(), getValue(), getLabel(),getExpirationTime());
	}

	
}
