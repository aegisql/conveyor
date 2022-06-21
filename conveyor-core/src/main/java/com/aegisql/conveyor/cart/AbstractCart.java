/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
	@Serial
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
	
	/** The priority. */
	protected final long priority;

	/** The future. */
	protected transient CompletableFuture<Boolean> future = null;
	
	/** The properties. */
	protected final Map<String, Object> properties = new HashMap<>();

	/** The load type. */
	protected final LoadType loadType;

	/** The cart creation nano timestamp. */
	private final long cartCreationNanoTimestamp = System.nanoTime();
	
	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param properties the properties
	 * @param loadType the load type
	 * @param priority the priority
	 */
	public AbstractCart(K k, V v, L label, long creation, long expiration, Map<String,Object> properties, LoadType loadType, long priority) {
		this.k              = k;
		this.v              = v;
		this.label          = label;
		this.creationTime   = creation;
		this.expirationTime = expiration;
		this.loadType       = loadType;
		if(properties != null) {
			this.properties.putAll(properties);
		}
		this.priority = priority;
	}

	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param expiration the expiration
	 * @param properties the properties
	 * @param loadType the load type
	 */
	public AbstractCart(K k, V v, L label, long creation, long expiration, Map<String,Object> properties, LoadType loadType) {
		this(k,v,label,creation,expiration,properties,loadType,0);
	}

	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param duration the duration
	 * @param properties the properties
	 * @param loadType the load type
	 * @param priority the priority
	 * @param dummy the dummy
	 */
	public AbstractCart(K k, V v, L label, long creation, long duration, Map<String,Object> properties, LoadType loadType, long priority, boolean dummy) {
		this(k,v,label,creation,creation+duration,properties,loadType,priority);
	}

	/**
	 * Instantiates a new abstract cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param label the label
	 * @param creation the creation
	 * @param duration the duration
	 * @param properties the properties
	 * @param loadType the load type
	 * @param dummy the dummy
	 */
	public AbstractCart(K k, V v, L label, long creation, long duration, Map<String,Object> properties, LoadType loadType, boolean dummy) {
		this(k,v,label,creation,creation+duration,properties,loadType,0);
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
			future = new CompletableFuture<>();
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
		return loadType
				+" [key=" + k 
				+ ", value=" + v 
				+ ", label=" + label 
				+ ", expirationTime=" + expirationTime
				+ (properties.size() == 0 ? "":", properties="+properties)
				+ "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getScrapConsumer()
	 */
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

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#addProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public <X> void addProperty(String name, X property) {
		properties.put(name, property);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getAllProperties()
	 */
	@Override
	public Map<String,Object> getAllProperties() {
		return properties;
	}
	
	/**
	 * Put all properties.
	 *
	 * @param other the other
	 */
	public void putAllProperties(Map<String,Object> other) {
		properties.putAll(other);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#clearProperty(java.lang.String)
	 */
	@Override
	public void clearProperty(String name) {
		properties.remove(name);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getLoadType()
	 */
	@Override
	public LoadType getLoadType() {
		return loadType;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getPriority()
	 */
	@Override
	public long getPriority() {
		return priority;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#getCartCreationNanoTime()
	 */
	public long getCartCreationNanoTime() {
		return cartCreationNanoTimestamp ;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Cart<K, ?, ?> cart) {
		if(cart==null) {
			return 0;
		}
		int cmpRes = Long.compare(cart.getPriority(),this.priority); //cart with higher priority go's first
		if(cmpRes==0) {
			cmpRes = Long.compare(this.cartCreationNanoTimestamp,cart.getCartCreationNanoTime()); //cart with same priority, first go's oldest
		}
		return cmpRes;
	}
	
}
