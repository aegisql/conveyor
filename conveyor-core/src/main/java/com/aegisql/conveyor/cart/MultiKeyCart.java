package com.aegisql.conveyor.cart;

import java.io.Serializable;
import java.util.function.Predicate;

import com.aegisql.conveyor.SerializableFunction;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class MultiKeyCart<K, V, L> extends AbstractCart<K, V, L> {
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
	public MultiKeyCart(V v, L label, long creation, long expiration) {
		super(null, v, label, creation,expiration,null,LoadType.MULTI_KEY_PART);
		this.addProperty("#FILTER", (Predicate)entry->true);
		this.addProperty("#CART_BUILDER", (SerializableFunction<K,Cart<K, ?, L>> & Serializable) key->{
			ShoppingCart<K,V,L> cart = new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
			cart.putAllProperties(this.getAllProperties());
			return cart;
		});
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
		super(null, v, label, creation,expiration,null,LoadType.MULTI_KEY_PART);
		this.addProperty("#FILTER", filter);
		this.addProperty("#CART_BUILDER", (SerializableFunction<K,Cart<K, ?, L>>) key->{
			ShoppingCart<K,V,L> cart = new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
			cart.putAllProperties(this.getAllProperties());
			return cart;
		});
	}

	public MultiKeyCart(Predicate<K> filter, V v, L label, long creation, long expiration, SerializableFunction<K,Cart<K, ?, L>> cartBuilder) {
		super(null, v, label, creation,expiration,null,LoadType.MULTI_KEY_PART);
		this.addProperty("#FILTER", filter);
		this.addProperty("#CART_BUILDER", cartBuilder);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		MultiKeyCart<K,V,L> cart = new MultiKeyCart<K,V,L>(getValue(), getLabel(),getCreationTime(),getExpirationTime());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}

	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		ShoppingCart<K,V,L> cart = new ShoppingCart<K,V,L>(key, getValue(), getLabel(),getExpirationTime());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}
	
}
