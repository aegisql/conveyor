package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// TODO: Auto-generated Javadoc
/**
 * The Class FutureLoader.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public final class FutureLoader<K,OUT> {

	/** The placer. */
	private final Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;
	
	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final K key;

	private final Map<String,Object> properties = new HashMap<>();

	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 */
	private FutureLoader(
			Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer,
			long creationTime, 
			long expirationTime, 
			long ttlMsec, 
			K key,
			Map<String,Object> properties) {
		this.placer = placer;
		this.creationTime = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param key the key
	 * @param dumb the dumb
	 */
	private FutureLoader(
			Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer,
			long creationTime, 
			long ttl, 
			K key,
			Map<String,Object> properties,
			boolean dumb) {
		this.placer = placer;
		this.creationTime = creationTime;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.key = key;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 */
	public FutureLoader(Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer) {
		this(placer,System.currentTimeMillis(),0,0,null,Collections.EMPTY_MAP);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> id(K k) {
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,k,properties);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  expirationTime(long et) {
		return new FutureLoader<K,OUT>(placer,creationTime,et,ttlMsec,key,properties);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  expirationTime(Instant instant) {
		return new FutureLoader<K,OUT>(placer,creationTime,instant.toEpochMilli(),ttlMsec,key,properties);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new FutureLoader<K,OUT>(placer,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,properties ,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  ttl(Duration duration) {
		return new FutureLoader<K,OUT>(placer,creationTime,duration.toMillis(),key,properties,true);
	}
	
	public FutureLoader<K,OUT> clearProperties() {
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,key,Collections.EMPTY_MAP);
	}

	public FutureLoader<K,OUT> clearProperty(String k) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.remove(k);
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,key,newMap);
	}

	public FutureLoader<K,OUT> addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,key,newMap);
	}

	public FutureLoader<K,OUT> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,key,newMap);
	}
	
	public <X> X getProperty(String key, Class<X> cls) {
		return (X) properties.get(key);
	}

	public Map<String,Object> getAllProperties() {
		return Collections.unmodifiableMap(properties);
	}

	
	/**
	 * Gets the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<OUT> get() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FutureLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + ", properties=" + properties + "]";
	}
	
}
