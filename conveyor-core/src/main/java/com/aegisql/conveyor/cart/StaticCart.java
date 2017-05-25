/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.cart;

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

	/** The create. */
	private final boolean create;
	
	/**
	 * Checks if is creates the.
	 *
	 * @return true, if is creates the
	 */
	public boolean isCreate() {
		return create;
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param create the create
	 */
	public StaticCart(V v, L label,boolean create) {
		super(null, v, label, 0);
		this.create = create;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new StaticCart<K,V,L>(getValue(), getLabel(),create);
	}

	/**
	 * To shopping cart.
	 *
	 * @param key the key
	 * @return the shopping cart
	 */
	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		return new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
	}
	
}
