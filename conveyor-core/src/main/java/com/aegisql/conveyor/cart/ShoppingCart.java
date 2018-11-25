package com.aegisql.conveyor.cart;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class ShoppingCart<K, V, L> extends AbstractCart<K, V, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;
	
	public ShoppingCart(K k, V v, L label, long creation, long expiration, Map<String,Object> properties, LoadType loadType,long priority) {
		super(k,v,label,creation,expiration,properties,loadType,priority);
	}

	public ShoppingCart(K k, V v, L label, long creation, long duration, Map<String,Object> properties, LoadType loadType, long priority, boolean dummy) {
		super(k,v,label,creation,duration,properties,loadType,priority,dummy);
	}


	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public ShoppingCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super(k, v, label, System.currentTimeMillis(),timeUnit.toMillis(ttl),null,LoadType.PART,true);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param expiration the expiration
	 */
	public ShoppingCart(K k, V v, L label, long expiration) {
		super(k, v, label, System.currentTimeMillis(),expiration,null,LoadType.PART);
		Objects.requireNonNull(k);
	}

	public ShoppingCart(K k, V v, L label, long creation, long expiration) {
		super(k, v, label, creation, expiration, null, LoadType.PART,0);
		Objects.requireNonNull(k);
	}

	public ShoppingCart(K k, V v, L label, long creation, long expiration,long priority) {
		super(k, v, label, creation, expiration, null, LoadType.PART,priority);
		Objects.requireNonNull(k);
	}

	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 */
	public ShoppingCart(K k, V v, L label) {
		super(k, v, label, System.currentTimeMillis(),0,null,LoadType.PART);
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,V,L> copy() {
		ShoppingCart<K,V,L> cart = new ShoppingCart<K,V,L>(getKey(), getValue(), getLabel(),getCreationTime(),getExpirationTime(),properties,loadType,getPriority());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}
	
}
