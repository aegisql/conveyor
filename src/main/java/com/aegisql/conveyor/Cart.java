package com.aegisql.conveyor;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class Cart<K,V,L> implements Delayed {
	
	private final K k;
	private final V v;
	private final L label;
	
	private final long created = System.currentTimeMillis();
	private final long expiration;
	
	public Cart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super();
		this.k          = k;
		this.v          = v;
		this.label      = label;
		this.expiration = created + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}
	
	public Cart(K k, V v, L label) {
		super();
		this.k = k;
		this.v = v;
		this.label = label;
		this.expiration = 0;
	}

	public Cart(K k, V v, L label, long expiration) {
		super();
		this.k = k;
		this.v = v;
		this.label = label;
		this.expiration = expiration;
	}

	public K getKey() {
		return k;
	}
	public V getValue() {
		return v;
	}
	public L getLabel() {
		return label;
	}
	public long getCreationTime() {
		return created;
	}
	public long getExpirationTime() {
		return expiration;
	}
	public boolean expired() {
		return expiration > 0 && expiration <= System.currentTimeMillis();
	}
	
	public <L1,V1> Cart<K,V1,L1> nextCart(V1 newValue,L1 newLabel) {
		return new Cart<>(this.getKey(),newValue,newLabel,this.getExpirationTime());
	}

	public <V1> Cart<K,V1,L> nextCart(V1 newValue) {
		return new Cart<>(this.getKey(), newValue, this.getLabel(), this.getExpirationTime());
	}

	@Override
	public String toString() {
		return "Cart [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", created=" + new Date(created) + 
				(expiration > 0 ? (", expires=" + new Date(expiration) ) : ", unexpireable") +
				 "]";
	}
	
	@Override
	public int compareTo(Delayed o) {
		if( this.created < ((Cart<?,?,?>)o).created) {
			return -1;
		}
		if( this.created > ((Cart<?,?,?>)o).created) {
			return +1;
		}
		return 0;
	}
	
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( this.expiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = this.expiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}
	
}
