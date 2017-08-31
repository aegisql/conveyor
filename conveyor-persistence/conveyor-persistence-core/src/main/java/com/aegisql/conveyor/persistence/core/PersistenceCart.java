package com.aegisql.conveyor.persistence.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;
import com.aegisql.conveyor.serial.SerializableFunction;

// TODO: Auto-generated Javadoc
/**
 * The Class PersistenceCart.
 *
 * @param <K> the key type
 */
public class PersistenceCart<K> extends AbstractCart<K,Cart<K,?,?>,SmartLabel<AcknowledgeBuilder<K>>> {

	/**
	 * Instantiates a new persistence cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param properties the properties
	 * @param loadType the load type
	 */
	private PersistenceCart(K k, Cart<K, ?, ?> v, SmartLabel<AcknowledgeBuilder<K>> label, long creation,
			long expiration, Map<String, Object> properties, LoadType loadType) {
		super(k, v, label, creation, expiration, properties, loadType);
	}

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, Cart<K, ?, ?>, SmartLabel<AcknowledgeBuilder<K>>> copy() {
		return new PersistenceCart<>(getKey(), getValue(), getLabel(), creationTime, expirationTime, getAllProperties(), getLoadType());
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param cart the cart
	 * @param label the label
	 * @return the persistence cart
	 */
	public static <K,L> PersistenceCart<K> of(Cart<K, ?, ?> cart, SmartLabel<AcknowledgeBuilder<K>> label) {
		Map<String,Object> properties = cart.getAllProperties();
		if(cart.getLoadType() == LoadType.MULTI_KEY_PART ) {
			Object oldCartBuilder = cart.getProperty("#CART_BUILDER",Object.class);
			properties.put("#CART_BUILDER", (SerializableFunction<K,Cart<K, ?, L>> ) key->{
				Map<String,Object> newProperties = new HashMap<>(properties);
				newProperties.put("#CART_BUILDER",oldCartBuilder);
				PersistenceCart<K> pc = new PersistenceCart<K>(key, cart, label,cart.getCreationTime(),cart.getExpirationTime(),newProperties,cart.getLoadType());
				return (Cart<K, ?, L>) pc;
			});
		}
		
		return new PersistenceCart<K>(cart.getKey(), cart, label, cart.getCreationTime(), cart.getExpirationTime(), cart.getAllProperties(), cart.getLoadType());
	}
	
}
