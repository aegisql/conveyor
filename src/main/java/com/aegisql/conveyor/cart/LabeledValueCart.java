package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.LabeledValue;

// TODO: Auto-generated Javadoc
/**
 * The Class LabeledValueCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class LabeledValueCart<K, V, L> extends AbstractCart<K, LabeledValue<L>, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param duration the duration
	 */
	public LabeledValueCart(K k, V v, L label, Duration duration) {
		super(k, new LabeledValue<L>(label,v), null, duration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param instant the instant
	 */
	public LabeledValueCart(K k, V v, L label, Instant instant) {
		super(k, new LabeledValue<L>(label,v), null, instant);
	}

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public LabeledValueCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super(k, new LabeledValue<L>(label,v), null, ttl, timeUnit);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public LabeledValueCart(K k, V v, L label, long expiration) {
		super(k, new LabeledValue<L>(label,v), null, expiration);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 */
	public LabeledValueCart(K k, V v, L label) {
		super(k, new LabeledValue<L>(label,v), null);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new labeled value cart.
	 *
	 * @param other the other
	 */
	public LabeledValueCart(Cart<K,V,L> other) {
		super(other.getKey(),new LabeledValue<L>(other.getLabel(),other.getValue()),null,other.getExpirationTime());
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, LabeledValue<L>, L> copy() {
		return new LabeledValueCart<K, LabeledValue<L>, L>(getKey(), getValue(), getLabel(), getExpirationTime());
	}
}
