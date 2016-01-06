package com.aegisql.conveyor.cart;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CreatingCart<K, B extends Supplier<?>, L> extends AbstractCart<K, Supplier<B>, L> {

	private static final long serialVersionUID = 4985202264573416558L;

	public CreatingCart(K k, Supplier<B> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
	}

	public CreatingCart(K k, Supplier<B> v, long expiration) {
		super(k, v, null, expiration);
	}

	public CreatingCart(K k, Supplier<B> v) {
		super(k, v, null);
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
