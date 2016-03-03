package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CreatingCart<K, B, L> extends AbstractCart<K, Supplier<Supplier<? extends B>>, L> {

	private static final long serialVersionUID = 4985202264573416558L;

	public CreatingCart(K k, Supplier<Supplier<? extends B>> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
	}

	public CreatingCart(K k, Supplier<Supplier<? extends B>> v, long expiration) {
		super(k, v, null, expiration);
	}

	public CreatingCart(K k, Supplier<Supplier<? extends B>> v) {
		super(k, v, null);
	}

	public CreatingCart(K k, Supplier<Supplier<? extends B>> b, Duration duration) {
		super(k, b,null, duration);
	}

	public CreatingCart(K k, Supplier<Supplier<? extends B>> b, Instant instant) {
		super(k, b,null, instant);
	}

	public CreatingCart(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, null, ttl, timeUnit);
	}

	public CreatingCart(K k, long expiration) {
		super(k, null, null, expiration);
	}

	public CreatingCart(K k) {
		super(k, null, null);
	}

	public CreatingCart(K k, Duration duration) {
		super(k, null,null, duration);
	}

	public CreatingCart(K k, Instant instant) {
		super(k, null,null, instant);
	}

	/**
	 * Next cart.
	 *
	 * @param <V1> the generic type
	 * @param newValue the new value
	 * @param newLabel the new label
	 * @return the cart
	 */
	public <V1> ShoppingCart<K,V1,L> shoppingCart(V1 newValue,L newLabel) {
		return new ShoppingCart<>(this.getKey(),newValue,newLabel,this.getExpirationTime());
	}
	
}
