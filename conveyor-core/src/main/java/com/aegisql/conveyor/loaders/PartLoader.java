package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.aegisql.conveyor.serial.SerializablePredicate;

// TODO: Auto-generated Javadoc
/**
 * The Class PartLoader.
 * @param <K> the key type
 *
 * @param <L> the generic type
 * @param <V> the value type
 * @param <OUT> the generic type
 * @param <F> the generic type
 */
public final class PartLoader<K,L,V,OUT,F> {

	/** The placer. */
	private final Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;
	
	/** The ttl msec. */
	public final long ttlMsec;

	/**  The priority. */
	public final long priority;

	/** The key. */
	public final K key;
	
	/** The key filter. */
	public final SerializablePredicate<K> filter;
	
	/** The label. */
	public final L label;
	
	/** The part value. */
	public final V partValue;
	
	/** The properties. */
	private final Map<String,Object> properties = new HashMap<>();
	
	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param priority the priority
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 * @param filter the filter
	 * @param properties the properties
	 */
	private PartLoader(
			Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, 
			long creationTime, 
			long expirationTime, 
			long ttlMsec, 
			long priority, 
			K key, 
			L label, 
			V value,
			SerializablePredicate<K> filter, 
			Map<String,Object> properties) {
		this.placer         = placer;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.key            = key;
		this.label          = label;
		this.partValue      = value;
		this.filter         = filter;
		this.priority       = priority;
		this.properties.putAll(properties);
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param priority the priority
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 * @param filter the filter
	 * @param properties the properties
	 * @param dumb the dumb
	 */
	private PartLoader(
			Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, 
			long creationTime, 
			long ttl, 
			long priority, 
			K key, 
			L label, 
			V value, 
			SerializablePredicate<K> filter, 
			Map<String,Object> properties, 
			boolean dumb) {
		this(placer,
				creationTime,
				creationTime + ttl,
				ttl,
				priority,
				key,
				label,
				value,
				filter,
				properties);
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,System.currentTimeMillis(),0,0,0,null,null,null,null,Collections.EMPTY_MAP);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> id(K k) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,k,label,partValue,null/*either id or filter*/,properties);
	}

	/**
	 * Foreach.
	 *
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> foreach() {
		return foreach(k->true);
	}

	/**
	 * Foreach.
	 *
	 * @param f the f
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> foreach(SerializablePredicate<K> f) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,null/*either id or filter*/,label,partValue,f,properties);
	}
	
	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> label(L l) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,l,partValue,filter,properties);
	}

	/**
	 * Value.
	 *
	 * @param <X> the generic type
	 * @param v the v
	 * @return the part loader
	 */
	public<X> PartLoader<K,L,X,OUT,F> value(X v) {
		return new PartLoader<K,L,X,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,label,v,filter,properties);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  expirationTime(long et) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,et,0,priority,key,label,partValue,filter,properties);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  creationTime(long ct) {
		return new PartLoader<K,L,V,OUT,F>(placer,ct,expirationTime,0,priority,key,label,partValue,filter,properties);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  expirationTime(Instant instant) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,instant.toEpochMilli(),0,priority,key,label,partValue,filter,properties);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  creationTime(Instant instant) {
		return new PartLoader<K,L,V,OUT,F>(placer,instant.toEpochMilli(),expirationTime,0,priority,key,label,partValue,filter,properties);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  ttl(long time, TimeUnit unit) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),priority,key,label,partValue,filter,properties ,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  ttl(Duration duration) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,duration.toMillis(),priority,key,label,partValue,filter,properties,true);
	}

	/**
	 * Priority.
	 *
	 * @param p the priority
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  priority(long p) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,p,key,label,partValue,filter,properties);
	}

	/**
	 * Clear properties.
	 *
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> clearProperties() {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,label,partValue,filter,Collections.EMPTY_MAP);
	}

	/**
	 * Clear property.
	 *
	 * @param k the k
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> clearProperty(String k) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.remove(k);
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,label,partValue,filter,newMap);
	}

	/**
	 * Adds the property.
	 *
	 * @param k the k
	 * @param v the v
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> addProperty(String k, Object v) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.put(k, v);
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,label,partValue,filter,newMap);
	}

	/**
	 * Adds the properties.
	 *
	 * @param moreProperties the more properties
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> addProperties(Map<String,Object> moreProperties) {
		Map<String,Object> newMap = new HashMap<>();
		newMap.putAll(properties);
		newMap.putAll(moreProperties);
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,priority,key,label,partValue,filter,newMap);
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
	 * Place.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<F> place() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PartLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", priority=" + priority + ", key=" + key + ", label=" + label + ", partValue=" + partValue + ", properties=" + properties + "]";
	}
	
}
