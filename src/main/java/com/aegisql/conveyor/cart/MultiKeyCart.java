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
public class MultiKeyCart<K, V, L> extends AbstractCart<K, V, L> implements Predicate<K> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	protected final Predicate<K> filter;
	
	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param creation the creation time
	 * @param expiration the expiration time
	 */
	public MultiKeyCart(V v, L label, long creation, long expiration) {
		super(null, v, label, expiration);
		filter = entry->true; //pass all by default
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param filter K key filtering predicate
	 * @param v the v
	 * @param label the label
	 * @param creation the creation time
	 * @param expiration the expiration time
	 */
	public MultiKeyCart(Predicate<K> filter, V v, L label, long creation, long expiration) {
		super(null, v, label, expiration);
		this.filter = filter;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		return new MultiKeyCart<K,V,L>(getValue(), getLabel(),getCreationTime(),getExpirationTime());
	}

	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		return new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
	}
	
	@Override
	public boolean test(K key) {
		return filter.test(key);
	}
	
}
