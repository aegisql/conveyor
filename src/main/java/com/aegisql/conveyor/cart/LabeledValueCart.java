package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.LabeledValue;

public class LabeledValueCart<K, V, L> extends AbstractCart<K, LabeledValue<L>, L> {

	private static final long serialVersionUID = 1L;

	public LabeledValueCart(K k, V v, L label, Duration duration) {
		super(k, new LabeledValue<L>(label,v), null, duration);
	}

	public LabeledValueCart(K k, V v, L label, Instant instant) {
		super(k, new LabeledValue<L>(label,v), null, instant);
	}

	public LabeledValueCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super(k, new LabeledValue<L>(label,v), null, ttl, timeUnit);
	}

	public LabeledValueCart(K k, V v, L label, long expiration) {
		super(k, new LabeledValue<L>(label,v), null, expiration);
	}

	public LabeledValueCart(K k, V v, L label) {
		super(k, new LabeledValue<L>(label,v), null);
	}

	public LabeledValueCart(Cart<K,V,L> other) {
		super(other.getKey(),new LabeledValue<L>(other.getLabel(),other.getValue()),null,other.getExpirationTime());
	}

	@Override
	public Cart<K, LabeledValue<L>, L> copy() {
		return new LabeledValueCart<K, LabeledValue<L>, L>(getKey(), getValue(), getLabel(), getExpirationTime());
	}
}
