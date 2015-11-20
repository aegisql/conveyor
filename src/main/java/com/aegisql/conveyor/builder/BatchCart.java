package com.aegisql.conveyor.builder;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.Cart;

public class BatchCart <V> extends
		Cart<String, V, BatchLabel<V>> {

	private static final long serialVersionUID = 7358052456949320913L;

	public BatchCart(String k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, new BatchLabel<V>(), ttl, timeUnit);
	}

	public BatchCart(String k, V v, long expiration) {
		super(k, v, new BatchLabel<V>(), expiration);
	}

	public BatchCart(String k, V v) {
		super(k, v, new BatchLabel<V>());
	}

	public BatchCart(V v, long ttl, TimeUnit timeUnit) {
		super("__BATCH__", v, new BatchLabel<V>(), ttl, timeUnit);
	}

	public BatchCart(V v, long expiration) {
		super("__BATCH__", v, new BatchLabel<V>(), expiration);
	}

	public BatchCart(V v) {
		super("__BATCH__", v, new BatchLabel<V>());
	}
	
}
