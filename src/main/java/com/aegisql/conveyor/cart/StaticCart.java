package com.aegisql.conveyor.cart;

import java.util.function.Predicate;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class StaticCart<K, V, L> extends AbstractCart<K, V, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param creation the creation time
	 * @param expiration the expiration time
	 */
	public StaticCart(V v, L label) {
		super(null, v, label, 0);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new StaticCart<K,V,L>(getValue(), getLabel());
	}

	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		return new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
	}
	
}
