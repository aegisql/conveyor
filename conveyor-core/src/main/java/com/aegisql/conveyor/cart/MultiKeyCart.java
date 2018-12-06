package com.aegisql.conveyor.cart;

import java.util.Map;

import com.aegisql.conveyor.serial.SerializablePredicate;

// TODO: Auto-generated Javadoc
/**
 * The Class ShoppingCart.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <L> the generic type
 */
public class MultiKeyCart<K, V, L> extends AbstractCart<K, Load<K,V>, L> {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4055225191822888396L;

	/**
	 * Instantiates a new multi key cart.
	 *
	 * @param filter the filter
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param loadType the load type
	 * @param properties the properties
	 * @param priority the priority
	 */
	public MultiKeyCart(SerializablePredicate<K> filter, V v, L label, long creation, long expiration, LoadType loadType,Map<String,Object> properties,long priority) {
		super(null, new Load<>(v, filter, loadType), label, creation,expiration,properties,LoadType.MULTI_KEY_PART,priority);
	}

	
	/**
	 * Instantiates a new shopping cart.
	 *
	 * @param v the v
	 * @param label the label
	 * @param creation the creation time
	 * @param expiration the expiration time
	 */
	public MultiKeyCart(V v, L label, long creation, long expiration) {
		super(null, new Load<>(v, x->true, LoadType.PART), label, creation,expiration,null,LoadType.MULTI_KEY_PART);
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
	public MultiKeyCart(SerializablePredicate<K> filter, V v, L label, long creation, long expiration) {
		super(null, new Load<>(v, filter, LoadType.PART), label, creation,expiration,null,LoadType.MULTI_KEY_PART);
	}

	/**
	 * Instantiates a new multi key cart.
	 *
	 * @param filter the filter
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param priority the priority
	 */
	public MultiKeyCart(SerializablePredicate<K> filter, V v, L label, long creation, long expiration,long priority) {
		super(null, new Load<>(v, filter, LoadType.PART), label, creation,expiration,null,LoadType.MULTI_KEY_PART,priority);
	}

	/**
	 * Instantiates a new multi key cart.
	 *
	 * @param filter the filter
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param loadType the load type
	 * @param priority the priority
	 */
	public MultiKeyCart(SerializablePredicate<K> filter, V v, L label, long creation, long expiration, LoadType loadType,long priority) {
		super(null, new Load<>(v, filter, loadType), label, creation,expiration,null,LoadType.MULTI_KEY_PART);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart <K,Load<K,V>,L> copy() {
		MultiKeyCart<K,V,L> cart = new MultiKeyCart<K,V,L>(getValue().getFilter(),getValue().getValue(), getLabel(),getCreationTime(),getExpirationTime(),getValue().getLoadType(),priority);
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}

	/**
	 * To shopping cart.
	 *
	 * @param key the key
	 * @return the shopping cart
	 */
	public ShoppingCart<K, V, L> toShoppingCart(K key) {
		ShoppingCart<K,V,L> cart = new ShoppingCart<K,V,L>(key, getValue().getValue(), getLabel(),getExpirationTime(),getPriority());
		cart.putAllProperties(this.getAllProperties());
		return cart;
	}
	
}
