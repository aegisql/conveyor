/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.cart;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class Cart.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 * @param <L>
 *            the generic type
 */
public abstract class AbstractCart<K, V, L> implements Cart<K, V, L> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5414733837801886611L;

	/** The k. */
	protected final K k;

	/** The v. */
	protected final V v;

	/** The label. */
	protected final L label;
	
	/** The creation time. */
	protected final long creationTime; 

	/** The expiration time. */
	protected final long expirationTime; 

	/** The future. */
	protected transient CompletableFuture<Boolean> future = null;
	
	protected final Map<String, Object> properties = new HashMap<>();
	
	/**
	 * Instantiates a new cart.
	 *
	 * @param k
	 *            the k
	 * @param v
	 *            the v
	 * @param label
	 *            the label
	 * @param ttl
	 *            the ttl
	 * @param timeUnit
	 *            the time unit
	 */
	public AbstractCart(K k, V v, L label, long ttl, TimeUnit timeUnit) {
		this.k = k;
		this.v = v;
		this.label = label;
		this.creationTime = System.currentTimeMillis();
		this.expirationTime = creationTime + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	/**
	 * Instantiates a new cart.
	 *
	 * @param k
	 *            the k
	 * @param v
	 *            the v
	 * @param label
	 *            the label
	 */
	public AbstractCart(K k, V v, L label) {
		this(k,v,label,System.currentTimeMillis(),0);
	}

	/**
	 * Instantiates a new cart.
	 *
	 * @param k
	 *            the k
	 * @param v
	 *            the v
	 * @param label
	 *            the label
	 * @param expiration
	 *            the expiration
	 */
	public AbstractCart(K k, V v, L label, long expiration) {
		this(k,v,label,System.currentTimeMillis(),expiration);
	}

	/**
	 * Instantiates a new cart.
	 *
	 * @param k
	 *            the k
	 * @param v
	 *            the v
	 * @param label
	 *            the label
	 * @param creation
	 *            the creation time
	 * @param expiration
	 *            the expiration time
	 */
	public AbstractCart(K k, V v, L label, long creation, long expiration) {
		this.k = k;
		this.v = v;
		this.label = label;
		this.creationTime   = creation;
		this.expirationTime = expiration;
	}

	
	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param duration the duration
	 */
	public AbstractCart(K k, V v, L label, Duration duration) {
		this.k = k;
		this.v = v;
		this.label = label;
		this.creationTime = System.currentTimeMillis();
		this.expirationTime = creationTime + duration.toMillis();
	}

	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param instant the instant
	 */
	public AbstractCart(K k, V v, L label, Instant instant) {
		this.k = k;
		this.v = v;
		this.label = label;
		this.creationTime = System.currentTimeMillis();
		this.expirationTime = instant.toEpochMilli();
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey() {
		return k;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public V getValue() {
		return v;
	}

	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public L getLabel() {
		return label;
	}

	/**
	 * Gets the creation time.
	 *
	 * @return the creation time
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	@Override
	public long getExpirationTime() {
		return expirationTime;
	}

	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return expirationTime > 0 && expirationTime < System.currentTimeMillis();
	}

	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getFuture()
	 */
	@Override
	public CompletableFuture<Boolean> getFuture() {
		if(future == null) {
			future = new CompletableFuture<Boolean>();
		}
		return future;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName()+" [key=" + k + ", value=" + v + ", label=" + label + ", expirationTime=" + expirationTime + "]";
	}

	@Override
	public ScrapConsumer<K, Cart<K, V, L>> getScrapConsumer() {
		return bin->{
			CompletableFuture<Boolean> f = bin.scrap.getFuture();
			if(bin.error !=null) {
				f.completeExceptionally(bin.error);
			} else {
				f.cancel(true);
			}
		};
	}

	@Override
	public <X> void addProperty(String name, X property) {
		properties.put(name, property);
	}

	@Override
	public <X> X getProperty(String name, Class<X> cls) {
		return (X) properties.get(name);
	}

	@Override
	public Map<String,Object> getAllProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	protected void putAllProperties(Map<String,Object> other) {
		properties.putAll(other);
	}

	@Override
	public void clearProperty(String name) {
		properties.remove(name);
	}	
	
}
