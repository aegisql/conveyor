package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

public class CreatingCart<K, B, L> extends AbstractCart<K, BuilderSupplier<B>, L> implements Supplier<BuilderSupplier<B>> {

	private static final long serialVersionUID = 4985202264573416558L;

	public CreatingCart(K k, BuilderSupplier<B> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
	}

	public CreatingCart(K k, BuilderSupplier<B> v, long expiration) {
		super(k, v, null, expiration);
	}

	public CreatingCart(K k, BuilderSupplier<B> v) {
		super(k, v, null);
	}

	public CreatingCart(K k, BuilderSupplier<B> b, Duration duration) {
		super(k, b,null, duration);
	}

	public CreatingCart(K k, BuilderSupplier<B> b, Instant instant) {
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

	@Override
	public BuilderSupplier<B> get() {
		return getValue();
	}
	
	@Override
	public Cart<K,BuilderSupplier<B>,L> copy() {
		return new CreatingCart<K,B,L>(getKey(),getValue(),getExpirationTime());
	}

}
