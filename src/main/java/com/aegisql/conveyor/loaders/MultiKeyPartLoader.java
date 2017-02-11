package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

// TODO: Auto-generated Javadoc
/**
 * The Class MultiKeyPartLoader.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <V> the value type
 * @param <OUT> the generic type
 * @param <F> the generic type
 */
public final class MultiKeyPartLoader<K,L,V,OUT,F> {

	/** The placer. */
	private final Function<MultiKeyPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;
	
	/** The ttl msec. */
	public final long ttlMsec;
	
	/** The key. */
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
	private MultiKeyPartLoader(Function<MultiKeyPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer,long creationTime, long expirationTime, long ttlMsec, Predicate<K> filter, L label, V value) {
		this.placer = placer;
		this.creationTime = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.filter = filter;
		this.label = label;
		this.partValue = value;
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
	private MultiKeyPartLoader(Function<MultiKeyPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer,long creationTime, long ttl, Predicate<K> filter, L label, V value, boolean dumb) {
		this.placer = placer;
		this.creationTime = creationTime;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.filter = filter;
		this.label = label;
		this.partValue = value;
	}

	/**
	 * Instantiates a new part loader.
	 *
	 * @param placer the placer
	 */
	public MultiKeyPartLoader(Function<MultiKeyPartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,System.currentTimeMillis(),0,0,k->false,null,null);
	}
	
	/**
	 * Id.
	 *
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F> foreach() {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,k->true,label,partValue);
	}

	/**
	 * Foreach
	 *
	 * @param filter the filtering predicate
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F> foreach(Predicate<K> filter) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,filter,label,partValue);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F> label(L l) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,expirationTime,ttlMsec,filter,l,partValue);
	}

	/**
	 * Value.
	 *
	 * @param <X> the generic type
	 * @param v the v
	 * @return the part loader
	 */
	public<X> MultiKeyPartLoader<K,L,X,OUT,F> value(X v) {
		return new MultiKeyPartLoader<K,L,X,OUT,F>(placer,creationTime,expirationTime,ttlMsec,filter,label,v);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F>  expirationTime(long et) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,et,0,filter,label,partValue);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F>  expirationTime(Instant instant) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,instant.toEpochMilli(),0,filter,label,partValue);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F>  ttl(long time, TimeUnit unit) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),filter,label,partValue ,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public MultiKeyPartLoader<K,L,V,OUT,F>  ttl(Duration duration) {
		return new MultiKeyPartLoader<K,L,V,OUT,F>(placer,creationTime,duration.toMillis(),filter,label,partValue,true);
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
		return "MultiKeyPartLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", label=" + label + ", partValue=" + partValue + "]";
	}
	
}
