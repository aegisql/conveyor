package com.aegisql.conveyor.utils.delay_line;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayLineCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class DelayLineCart<K, V> extends ShoppingCart<K, V, SmartLabel<DelayLineBuilder<V>>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6377776104535576600L;

	/**
	 * Instantiates a new delay line cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public DelayLineCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, DelayLineCart.getAddLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new delay line cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public DelayLineCart(K k, V v, long expiration) {
		super(k, v, DelayLineCart.getAddLabel(), expiration);
	}

	/**
	 * Instantiates a new delay line cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public DelayLineCart(K k, V v) {
		super(k, v, DelayLineCart.getAddLabel());
	}

	/**
	 * Gets the adds the label.
	 *
	 * @param <T> the generic type
	 * @return the adds the label
	 */
	private static <T> SmartLabel<DelayLineBuilder<T>> getAddLabel() {
		return new SmartLabel<DelayLineBuilder<T>>() {
			private static final long serialVersionUID = -483892404975143794L;
			@Override
			public BiConsumer<DelayLineBuilder<T>, Object> get() {
				BiConsumer<DelayLineBuilder<T>, T> bc = DelayLineBuilder::add;
				return (BiConsumer<DelayLineBuilder<T>, Object>) bc;
			}
		};
	}
	
}
