package com.aegisql.conveyor.utils.batch;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

public class BatchCart <V> extends ShoppingCart<String, V, SmartLabel<BatchCollectingBuilder<V>>> {

	private static final long serialVersionUID = 7358052456949320913L;

	public BatchCart(String k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, BatchCart.<V>getAddLabel(), ttl, timeUnit);
	}

	public BatchCart(String k, V v, long expiration) {
		super(k, v, BatchCart.<V>getAddLabel(), expiration);
	}

	public BatchCart(String k, V v) {
		super(k, v, BatchCart.<V>getAddLabel());
	}

	public BatchCart(V v, long ttl, TimeUnit timeUnit) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel(), ttl, timeUnit);
	}

	public BatchCart(V v, long expiration) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel(), expiration);
	}

	public BatchCart(V v) {
		super("_BATCH_"+v.getClass().getName(), v, BatchCart.<V>getAddLabel() );
	}
		
	private static <V> SmartLabel<BatchCollectingBuilder<V>> getAddLabel() {
		return new SmartLabel<BatchCollectingBuilder<V>>(){
			private static final long serialVersionUID = 1L;
			public BiConsumer<BatchCollectingBuilder<V>, Object> getSetter() {
				BiConsumer<BatchCollectingBuilder<V>, V> consumer = BatchCollectingBuilder::add;
				return (BiConsumer<BatchCollectingBuilder<V>, Object>)consumer;
			}
			public String toString() {
				return "BATCH";
			}
			};
	}

}
