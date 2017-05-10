package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

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
	
	/** The key. */
	public final K key;
	
	/** The key filter. */
	public final Predicate<K> filter;
	
	/** The label. */
	public final L label;
	
	/** The part value. */
	public final V partValue;
	
	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 */
	private PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long creationTime, long expirationTime, long ttlMsec, K key, L label, V value,Predicate<K> filter) {
		this.placer         = placer;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.key            = key;
		this.label          = label;
		this.partValue      = value;
		this.filter         = filter;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 * @param ttl the ttl
	 * @param key the key
	 * @param label the label
	 * @param value the value
	 * @param dumb the dumb
	 */
	private PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long creationTime, long ttl, K key, L label, V value, Predicate<K> filter, boolean dumb) {
		this.placer         = placer;
		this.creationTime   = creationTime;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec        = ttl;
		this.key            = key;
		this.label          = label;
		this.partValue      = value;
		this.filter         = filter;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,System.currentTimeMillis(),0,0,null,null,null,null);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> id(K k) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,k,label,partValue,null/*either id or filter*/);
	}

	public PartLoader<K,L,V,OUT,F> foreach() {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,null/*either id or filter*/,label,partValue,k->true);
	}

	public PartLoader<K,L,V,OUT,F> foreach(Predicate<K> f) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,null/*either id or filter*/,label,partValue,f);
	}
	
	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F> label(L l) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,key,l,partValue,filter);
	}

	/**
	 * Value.
	 *
	 * @param <X> the generic type
	 * @param v the v
	 * @return the part loader
	 */
	public<X> PartLoader<K,L,X,OUT,F> value(X v) {
		return new PartLoader<K,L,X,OUT,F>(placer,creationTime,expirationTime,ttlMsec,key,label,v,filter);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  expirationTime(long et) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,et,0,key,label,partValue,filter);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  expirationTime(Instant instant) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,instant.toEpochMilli(),0,key,label,partValue,filter);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  ttl(long time, TimeUnit unit) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,label,partValue,filter ,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public PartLoader<K,L,V,OUT,F>  ttl(Duration duration) {
		return new PartLoader<K,L,V,OUT,F>(placer,creationTime,duration.toMillis(),key,label,partValue,filter,true);
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
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + ", label=" + label + ", partValue=" + partValue + "]";
	}
	
}
