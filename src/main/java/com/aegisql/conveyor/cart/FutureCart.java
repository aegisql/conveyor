package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FutureCart<K, B, L> extends AbstractCart<K, CompletableFuture<B>, L> implements Supplier<CompletableFuture<B>> {

	private static final long serialVersionUID = 4985202262573416558L;

	public FutureCart(K k, CompletableFuture<B> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
	}

	public FutureCart(K k, CompletableFuture<B> v, long expiration) {
		super(k, v, null, expiration);
	}

	public FutureCart(K k, CompletableFuture<B> v) {
		super(k, v, null);
	}

	public FutureCart(K k, CompletableFuture<B> v, Duration duration) {
		super(k, v,null, duration);
	}

	public FutureCart(K k, CompletableFuture<B> v, Instant instant) {
		super(k, v, null, instant);
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
	public CompletableFuture<B> get() {
		return getValue();
	}

	@Override
	public Cart<K, CompletableFuture<B>, L> copy() {
		return new FutureCart<K, B, L>(getKey(), getValue(), getExpirationTime());
	}
	
}
