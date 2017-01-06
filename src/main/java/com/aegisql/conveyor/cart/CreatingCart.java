package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CreatingCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class CreatingCart<K, B, L> extends AbstractCart<K, BuilderSupplier<B>, L> implements Supplier<BuilderSupplier<B>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202264573416558L;

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CreatingCart(K k, BuilderSupplier<B> v, long ttl, TimeUnit timeUnit) {
		super(k, v, null, ttl, timeUnit);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public CreatingCart(K k, BuilderSupplier<B> v, long expiration) {
		super(k, v, null, expiration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public CreatingCart(K k, BuilderSupplier<B> v) {
		super(k, v, null);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param b the b
	 * @param duration the duration
	 */
	public CreatingCart(K k, BuilderSupplier<B> b, Duration duration) {
		super(k, b,null, duration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param b the b
	 * @param instant the instant
	 */
	public CreatingCart(K k, BuilderSupplier<B> b, Instant instant) {
		super(k, b,null, instant);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CreatingCart(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, null, ttl, timeUnit);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public CreatingCart(K k, long expiration) {
		super(k, null, null, expiration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 */
	public CreatingCart(K k) {
		super(k, null, null);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param duration the duration
	 */
	public CreatingCart(K k, Duration duration) {
		super(k, null,null, duration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param instant the instant
	 */
	public CreatingCart(K k, Instant instant) {
		super(k, null,null, instant);
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
	public BuilderSupplier<B> get() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K,BuilderSupplier<B>,L> copy() {
		return new CreatingCart<K,B,L>(getKey(),getValue(),getExpirationTime());
	}

}
