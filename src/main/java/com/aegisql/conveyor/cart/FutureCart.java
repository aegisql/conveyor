package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class FutureCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class FutureCart<K, B, L> extends AbstractCart<K, CompletableFuture<B>, L> implements Supplier<CompletableFuture<B>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202262573416558L;

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public FutureCart(K k, CompletableFuture<B> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public FutureCart(K k, CompletableFuture<B> v, long expiration) {
		super(k, v, null, expiration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public FutureCart(K k, CompletableFuture<B> v) {
		super(k, v, null);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param duration the duration
	 */
	public FutureCart(K k, CompletableFuture<B> v, Duration duration) {
		super(k, v,null, duration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param instant the instant
	 */
	public FutureCart(K k, CompletableFuture<B> v, Instant instant) {
		super(k, v, null, instant);
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
	public <V1> ShoppingCart<K,V1,L> shoppingCart(V1 newValue,L newLabel) {
		return new ShoppingCart<>(this.getKey(),newValue,newLabel,this.getExpirationTime());
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public CompletableFuture<B> get() {
		return getValue();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, CompletableFuture<B>, L> copy() {
		return new FutureCart<K, B, L>(getKey(), getValue(), getExpirationTime());
	}
	
}
