package com.aegisql.conveyor.builder;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.cart.AbstractCart;

public class CollectionCompleteCart <K,V> extends AbstractCart<K, V, CollectionBuilderLabel<V>> {

	private static final long serialVersionUID = -4120525801809562774L;

	public CollectionCompleteCart(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), ttl, timeUnit);
	}

	public CollectionCompleteCart(K k, long expiration) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), expiration);
	}

	public CollectionCompleteCart(K k) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel());
	}

}
