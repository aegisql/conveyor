package com.aegisql.conveyor.cart;

import java.util.function.Function;
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
	
	protected final Function<K,Cart<K, ?, L>> cartBuilder;
	
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
		this.filter = entry->true; //pass all by default
		this.cartBuilder = this::toShoppingCart;
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
		this.cartBuilder = this::toShoppingCart;
	}

	public MultiKeyCart(Predicate<K> filter, V v, L label, long creation, long expiration, Function<K,Cart<K, ?, L>> cartBuilder) {
		super(null, v, label, expiration);
		this.filter = filter;
		this.cartBuilder = cartBuilder;
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
	
	public Function<K,Cart<K, ?, L>> cartBuilder() {
		return cartBuilder;
	}
	
}
