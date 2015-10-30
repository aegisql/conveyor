package com.aegisql.conveyor;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Cart<K,V,L> {
	
	private final K k;
	private final V v;
	private final L label;
	
	private final long created = System.currentTimeMillis();
	private final long expiration;
	
	
	public Cart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		super();
		this.k = k;
		this.v = v;
		this.label = label;
		switch(timeUnit) {
		case NANOSECONDS:
			this.expiration = created + ttl / 1_000_000; 
			break;
		case MICROSECONDS:
			this.expiration = created + ttl / 1_000; 
			break;
		case MILLISECONDS:
			this.expiration = created + ttl; 
			break;
		case SECONDS:
			this.expiration = created + ttl * 1000; 
			break;
		case MINUTES:
			this.expiration = created + ttl * 60 * 1000; 
			break;
		case HOURS:
			this.expiration = created + ttl * 60 * 60 * 1000; 
			break;
		case DAYS:
			this.expiration = created + ttl * 24 * 60 * 60 * 1000; 
			break;
		default:
			throw new RuntimeException("Unexpected Time Unit "+timeUnit);
		}
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
		return expiration > 0 && expiration > System.currentTimeMillis();
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
	
}
