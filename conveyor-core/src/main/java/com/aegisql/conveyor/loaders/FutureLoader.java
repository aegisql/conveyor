package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import com.aegisql.conveyor.Conveyor;

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

	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();
	
	/**  The priority. */
	public final long priority;


	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 * @param properties the properties
	 * @param priority the priority
	 */
	private FutureLoader(
			Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer,
			long creationTime, 
			long expirationTime, 
			long ttlMsec, 
			K key,
			Map<String,Object> properties,
			long priority) {
		this.placer = placer;
		this.creationTime = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.priority = priority;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param key the key
	 * @param properties the properties
	 * @param priority the priority
	 * @param dumb the dumb
	 */
	@SuppressWarnings("SameParameterValue")
	private FutureLoader(
			Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer,
			long creationTime, 
			long ttl, 
			K key,
			Map<String,Object> properties,
			long priority,
			boolean dumb) {
		this(placer,
				creationTime,
				creationTime + ttl,
				ttl,
				key,
				properties,
				priority);
	}

	/**
	 * Instantiates a new future loader.
	 *
	 * @param placer the placer
	 */
	public FutureLoader(Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer) {
		this(placer,System.currentTimeMillis(),0,0,null,Collections.EMPTY_MAP,0);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> id(K k) {
		return new FutureLoader<>(placer, creationTime, expirationTime, ttlMsec, k, properties, priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  expirationTime(long et) {
		return new FutureLoader<>(placer, creationTime, et, ttlMsec, key, properties, priority);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  creationTime(long ct) {
		return new FutureLoader<>(placer, ct, expirationTime, ttlMsec, key, properties, priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  expirationTime(Instant instant) {
		return new FutureLoader<>(placer, creationTime, instant.toEpochMilli(), ttlMsec, key, properties, priority);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  creationTime(Instant instant) {
		return new FutureLoader<>(placer, instant.toEpochMilli(), expirationTime, ttlMsec, key, properties, priority);
	}

	/**
	 * Priority.
	 *
	 * @param p the p
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  priority(long p) {
		return new FutureLoader<>(placer, creationTime, expirationTime, ttlMsec, key, properties, p);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new FutureLoader<>(placer, creationTime, TimeUnit.MILLISECONDS.convert(time, unit), key, properties, priority, true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the future loader
	 */
	public FutureLoader<K,OUT>  ttl(Duration duration) {
		return new FutureLoader<>(placer, creationTime, duration.toMillis(), key, properties, priority, true);
	}
	
	/**
	 * Clear properties.
	 *
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> clearProperties() {
		return new FutureLoader<K,OUT>(placer,creationTime,expirationTime,ttlMsec,key,Collections.EMPTY_MAP,priority);
	}

	/**
	 * Clear property.
	 *
	 * @param k the k
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> clearProperty(String k) {
		Map<String, Object> newMap = new HashMap<>(properties);
		newMap.remove(k);
		return new FutureLoader<>(placer, creationTime, expirationTime, ttlMsec, key, newMap, priority);
	}

	/**
	 * Adds the property.
	 *
	 * @param k the k
	 * @param v the v
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> addProperty(String k, Object v) {
		Map<String, Object> newMap = new HashMap<>(properties);
		newMap.put(k, v);
		return new FutureLoader<>(placer, creationTime, expirationTime, ttlMsec, key, newMap, priority);
	}

	/**
	 * Adds the properties.
	 *
	 * @param moreProperties the more properties
	 * @return the future loader
	 */
	public FutureLoader<K,OUT> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new FutureLoader<>(placer, creationTime, expirationTime, ttlMsec, key, newMap, priority);
	}
	
	/**
	 * Gets the property.
	 *
	 * @param <X> the generic type
	 * @param key the key
	 * @param cls the cls
	 * @return the property
	 */
	public <X> X getProperty(String key, Class<X> cls) {
		return (X) properties.get(key);
	}

	/**
	 * Gets the all properties.
	 *
	 * @return the all properties
	 */
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
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", priority="+priority+", key=" + key + ", properties=" + properties + "]";
	}
	
	public static <K,OUT> FutureLoader<K,OUT> byConveyorName(String name) {
		return Conveyor.byName(name).future();
	}

	public static <K,OUT> Supplier<FutureLoader<K,OUT>> lazySupplier(String name) {
		return new Supplier<>() {
            FutureLoader<K, OUT> fl;

            @Override
            public FutureLoader<K, OUT> get() {
                if (fl == null) {
                    Conveyor<K, ?, OUT> c = Conveyor.byName(name);
                    if (c != null) {
                        fl = c.future();
                    }
                }
                return fl;
            }
        };
	}

}
