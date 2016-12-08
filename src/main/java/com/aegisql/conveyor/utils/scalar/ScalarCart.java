package com.aegisql.conveyor.utils.scalar;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class ScalarCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ScalarCart<K, V> extends ShoppingCart<K, V, SmartLabel<ScalarConvertingBuilder<V, ?>>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -6377776104555576600L;

	/**
	 * Instantiates a new scalar cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public ScalarCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, ScalarCart.getAddLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new scalar cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public ScalarCart(K k, V v, long expiration) {
		super(k, v, ScalarCart.getAddLabel(), expiration);
	}

	/**
	 * Instantiates a new scalar cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public ScalarCart(K k, V v) {
		super(k, v, ScalarCart.getAddLabel());
	}

	/**
	 * Gets the adds the label.
	 *
	 * @param <T> the generic type
	 * @return the adds the label
	 */
	private static <T> SmartLabel<ScalarConvertingBuilder<T, ?>> getAddLabel() {
		return new SmartLabel<ScalarConvertingBuilder<T, ?>>() {
			private static final long serialVersionUID = -4838924049752143794L;
			@Override
			public BiConsumer<ScalarConvertingBuilder<T, ?>, Object> get() {
				BiConsumer<ScalarConvertingBuilder<T, ?>, T> bc = ScalarConvertingBuilder::add;
				return (BiConsumer<ScalarConvertingBuilder<T, ?>, Object>) bc;
			}
		};
	}
	
}
