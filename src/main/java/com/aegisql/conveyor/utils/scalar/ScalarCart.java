package com.aegisql.conveyor.utils.scalar;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ScalarCart<K, V> extends ShoppingCart<K, V, SmartLabel<ScalarConvertingBuilder<V, ?>>> {

	private static final long serialVersionUID = -6377776104555576600L;

	public ScalarCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, ScalarCart.getAddLabel(), ttl, timeUnit);
	}

	public ScalarCart(K k, V v, long expiration) {
		super(k, v, ScalarCart.getAddLabel(), expiration);
	}

	public ScalarCart(K k, V v) {
		super(k, v, ScalarCart.getAddLabel());
	}

	private static <T> SmartLabel<ScalarConvertingBuilder<T, ?>> getAddLabel() {
		return new SmartLabel<ScalarConvertingBuilder<T, ?>>() {
			private static final long serialVersionUID = -4838924049752143794L;
			@Override
			public BiConsumer<ScalarConvertingBuilder<T, ?>, Object> get() {
				BiConsumer<ScalarConvertingBuilder<T, ?>, T> bc = ScalarConvertingBuilder::add;
				return (BiConsumer<ScalarConvertingBuilder<T, ?>, Object>) bc;
			}
		
		};
	}
	
}
