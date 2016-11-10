package com.aegisql.conveyor.utils.batch;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class BatchCart.
 *
 * @param <V> the value type
 */
public class BatchCart <V> extends ShoppingCart<String, V, SmartLabel<BatchCollectingBuilder<V>>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7358052456949320913L;

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public BatchCart(String k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, BatchCart.<V>getAddLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public BatchCart(String k, V v, long expiration) {
		super(k, v, BatchCart.<V>getAddLabel(), expiration);
	}

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public BatchCart(String k, V v) {
		super(k, v, BatchCart.<V>getAddLabel());
	}

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public BatchCart(V v, long ttl, TimeUnit timeUnit) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param v the v
	 * @param expiration the expiration
	 */
	public BatchCart(V v, long expiration) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel(), expiration);
	}

	/**
	 * Instantiates a new batch cart.
	 *
	 * @param v the v
	 */
	public BatchCart(V v) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel() );
	}
		
	/**
	 * Gets the adds the label.
	 *
	 * @param <V> the value type
	 * @return the adds the label
	 */
	private static <V> SmartLabel<BatchCollectingBuilder<V>> getAddLabel() {
		return new SmartLabel<BatchCollectingBuilder<V>>(){
			private static final long serialVersionUID = 1L;
			public BiConsumer<BatchCollectingBuilder<V>, Object> get() {
				BiConsumer<BatchCollectingBuilder<V>, V> consumer = BatchCollectingBuilder::add;
				return (BiConsumer<BatchCollectingBuilder<V>, Object>)consumer;
			}
			public String toString() {
				return "BATCH";
			}
			};
	}

}
