package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class BuilderLoader.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 * @param <F> the generic type
 */
public final class BuilderLoader<K,OUT,F> {

	/** The placer. */
	private final Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer;

	/** The future placer. */
	private final Function<BuilderLoader<K,OUT,F>, CompletableFuture<OUT>> futurePlacer;

	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final K key;
	
	/** The value. */
	public final BuilderSupplier<OUT> value;

	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();
	
	/**  The priority. */
	public final long priority;

	
	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 * @param futurePlacer the future placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 * @param value the value
	 * @param properties the properties
	 * @param priority the priority
	 */
	private BuilderLoader(
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer,
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<OUT>> futurePlacer,
			long creationTime,
			long expirationTime,
			long ttlMsec,
			K key, 
			BuilderSupplier<OUT> value,
			Map<String,Object> properties,
			long priority) {
		this.placer = placer;
		this.futurePlacer = futurePlacer;
		this.creationTime = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.value = value;
		this.priority = priority;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 * @param futurePlacer the future placer
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param key the key
	 * @param value the value
	 * @param properties the properties
	 * @param priority the priority
	 * @param dumb the dumb
	 */
	private BuilderLoader(
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer,
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<OUT>> futurePlacer,
			long creationTime,
			long ttl,
			K key,
			BuilderSupplier<OUT> value,
			Map<String,Object> properties,
			long priority,
			boolean dumb) {
		this(placer,
				futurePlacer,
				creationTime,
				creationTime + ttl,
				ttl,
				key,
				value,
				properties,
				priority);
	}

	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 * @param futurePlacer the future placer
	 */
	public BuilderLoader(
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer,
			Function<BuilderLoader<K,OUT,F>, CompletableFuture<OUT>> futurePlacer) {
		this(placer,futurePlacer,System.currentTimeMillis(),0,0,null,null,Collections.EMPTY_MAP,0);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> id(K k) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,k,value,properties,priority);
	}

	/**
	 * Supplier.
	 *
	 * @param v the v
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> supplier(BuilderSupplier<OUT> v) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,v,properties,priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  expirationTime(long et) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,et,ttlMsec,key,value,properties,priority);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  creationTime(long ct) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,ct,expirationTime,ttlMsec,key,value,properties,priority);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  expirationTime(Instant instant) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,instant.toEpochMilli(),ttlMsec,key,value,properties,priority);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  creationTime(Instant instant) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,instant.toEpochMilli(),expirationTime,ttlMsec,key,value,properties,priority);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  ttl(long time, TimeUnit unit) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,value,properties,priority,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  ttl(Duration duration) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,duration.toMillis(),key,value,properties,priority,true);
	}
	
	/**
	 * Priority.
	 *
	 * @param p the p
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  priority(long p) {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,value,properties,p);
	}
	
	/**
	 * Clear properties.
	 *
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> clearProperties() {
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,value,Collections.EMPTY_MAP,priority);
	}

	/**
	 * Clear property.
	 *
	 * @param k the k
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  clearProperty(String k) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.remove(k);
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,value,newMap,priority);
	}

	/**
	 * Adds the property.
	 *
	 * @param k the k
	 * @param v the v
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,value,newMap,priority);
	}

	/**
	 * Adds the properties.
	 *
	 * @param moreProperties the more properties
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new BuilderLoader<K,OUT,F>(placer,futurePlacer,creationTime,expirationTime,ttlMsec,key,value,newMap,priority);
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
	 * Creates the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<F> create() {
		return placer.apply(this);
	}

	/**
	 * Creates the future.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<OUT> createFuture() {
		return futurePlacer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuilderLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + ", priority="+priority+", properties=" + properties + "]";
	}
	
}
