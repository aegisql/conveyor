package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;

import java.io.Serial;
import java.util.Map;

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
	@Serial
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

		LoadType loadType = switch (cart.getLoadType()) {
			case STATIC_PART, MULTI_KEY_PART, RESULT_CONSUMER -> LoadType.PART;
			default -> cart.getLoadType();
		};

		return new PersistenceCart<>(cart.getKey(), cart, label, cart.getCreationTime(), cart.getExpirationTime(), cart.getAllProperties(), loadType);
	}
	
}
