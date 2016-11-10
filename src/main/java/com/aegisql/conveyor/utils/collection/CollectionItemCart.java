package com.aegisql.conveyor.utils.collection;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionItemCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class CollectionItemCart <K,V> extends AbstractCart<K, V, CollectionBuilderLabel<V>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -4120525801809562774L;

	/**
	 * Instantiates a new collection item cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CollectionItemCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, CollectionBuilderLabel.addItemLabel(), ttl, timeUnit);
	}

	/**
	 * Instantiates a new collection item cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public CollectionItemCart(K k, V v, long expiration) {
		super(k, v, CollectionBuilderLabel.addItemLabel(), expiration);
	}

	/**
	 * Instantiates a new collection item cart.
	 *
	 * @param k the k
	 * @param v the v
	 */
	public CollectionItemCart(K k, V v) {
		super(k, v, CollectionBuilderLabel.addItemLabel());
	}

	/**
	 * Next item.
	 *
	 * @param newValue the new value
	 * @return the collection item cart
	 */
	public CollectionItemCart<K,V> nextItem(V newValue) {
		return new CollectionItemCart<>(this.getKey(), newValue, this.getExpirationTime());
	}
	
	/**
	 * Compelte.
	 *
	 * @return the collection complete cart
	 */
	public CollectionCompleteCart<K,V> compelte() {
		return new CollectionCompleteCart<>(this.getKey(), this.getExpirationTime());		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, V, CollectionBuilderLabel<V>> copy() {
		return new CollectionItemCart<K,V>(getKey(), getValue(), getExpirationTime());
	}

}
