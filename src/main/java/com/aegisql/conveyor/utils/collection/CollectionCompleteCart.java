package com.aegisql.conveyor.utils.collection;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionCompleteCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class CollectionCompleteCart <K,V> extends AbstractCart<K, V, CollectionBuilderLabel<V>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -4120525801809562774L;

	/**
	 * Instantiates a new collection complete cart.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CollectionCompleteCart(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new collection complete cart.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public CollectionCompleteCart(K k, long expiration) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), expiration);
	}

	/**
	 * Instantiates a new collection complete cart.
	 *
	 * @param k the k
	 */
	public CollectionCompleteCart(K k) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, V, CollectionBuilderLabel<V>> copy() {
		return new CollectionCompleteCart<K,V>(getKey(), getExpirationTime());
	}

}
