package com.aegisql.conveyor.persistence.core;

import java.util.Map;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.ack.AcknowledgeBuilder;

public class PersistenceCart<K> extends AbstractCart<K,Cart<K,?,?>,SmartLabel<AcknowledgeBuilder<K>>> {

	private PersistenceCart(K k, Cart<K, ?, ?> v, SmartLabel<AcknowledgeBuilder<K>> label, long creation,
			long expiration, Map<String, Object> properties, LoadType loadType) {
		super(k, v, label, creation, expiration, properties, loadType);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public Cart<K, Cart<K, ?, ?>, SmartLabel<AcknowledgeBuilder<K>>> copy() {
		return new PersistenceCart<>(getKey(), getValue(), getLabel(), creationTime, expirationTime, getAllProperties(), getLoadType());
	}

	public static <K,L> PersistenceCart<K> of(Cart<K, ?, ?> cart, SmartLabel<AcknowledgeBuilder<K>> label) {
		return new PersistenceCart<K>(cart.getKey(), cart, label, cart.getCreationTime(), cart.getExpirationTime(), cart.getAllProperties(), cart.getLoadType());
	}
	
}
