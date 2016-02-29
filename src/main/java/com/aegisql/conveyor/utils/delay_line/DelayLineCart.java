package com.aegisql.conveyor.utils.delay_line;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.ShoppingCart;

public class DelayLineCart<K, V> extends ShoppingCart<K, V, SmartLabel<DelayLineBuilder<V>>> {

	private static final long serialVersionUID = -6377776104535576600L;

	public DelayLineCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, DelayLineCart.getAddLabel(), ttl, timeUnit);
	}

	public DelayLineCart(K k, V v, long expiration) {
		super(k, v, DelayLineCart.getAddLabel(), expiration);
	}

	public DelayLineCart(K k, V v) {
		super(k, v, DelayLineCart.getAddLabel());
	}

	private static <T> SmartLabel<DelayLineBuilder<T>> getAddLabel() {
		return new SmartLabel<DelayLineBuilder<T>>() {
			private static final long serialVersionUID = -483892404975143794L;
			@Override
			public BiConsumer<DelayLineBuilder<T>, Object> get() {
				BiConsumer<DelayLineBuilder<T>, T> bc = DelayLineBuilder::add;
				return (BiConsumer<DelayLineBuilder<T>, Object>) bc;
			}
		};
	}
	
}
