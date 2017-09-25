package com.aegisql.conveyor.persistence.core;

import java.util.Map;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;

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

		LoadType loadType;
		
		switch(cart.getLoadType()) {
			case STATIC_PART:
			case MULTI_KEY_PART:
			case RESULT_CONSUMER:
				loadType = LoadType.PART;
				break;
			default:
				loadType = cart.getLoadType();
		}
		
		return new PersistenceCart<K>(cart.getKey(), cart, label, cart.getCreationTime(), cart.getExpirationTime(), cart.getAllProperties(), loadType);
	}
	
}
