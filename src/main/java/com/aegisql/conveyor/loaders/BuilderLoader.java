package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
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
	
	/** The creation time. */
	public final long creationTime = System.currentTimeMillis(); 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	private final long ttlMsec;

	/** The key. */
	public final K key;
	
	/** The value. */
	public final BuilderSupplier<OUT> value;

	
	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 * @param expirationTime the expiration time
	 * @param key the key
	 * @param value the value
	 */
	private BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer, long expirationTime, long ttlMsec,K key, BuilderSupplier<OUT> value) {
		this.placer = placer;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.value = value;
	}

	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 * @param ttl the ttl
	 * @param key the key
	 * @param value the value
	 * @param dumb the dumb
	 */
	private BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer, long ttl, K key, BuilderSupplier<OUT> value, boolean dumb) {
		this.placer = placer;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.key = key;
		this.value = value;
	}

	/**
	 * Instantiates a new builder loader.
	 *
	 * @param placer the placer
	 */
	public BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,0,0,null,null);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> id(K k) {
		return new BuilderLoader<K,OUT,F>(placer,expirationTime,ttlMsec,k,value);
	}

	/**
	 * Supplier.
	 *
	 * @param v the v
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F> supplier(BuilderSupplier<OUT> v) {
		return new BuilderLoader<K,OUT,F>(placer,expirationTime,ttlMsec,key,v);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  expirationTime(long et) {
		return new BuilderLoader<K,OUT,F>(placer,et,ttlMsec,key,value);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  expirationTime(Instant instant) {
		return new BuilderLoader<K,OUT,F>(placer,instant.toEpochMilli(),ttlMsec,key,value);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  ttl(long time, TimeUnit unit) {
		return new BuilderLoader<K,OUT,F>(placer,TimeUnit.MILLISECONDS.convert(time, unit),key,value ,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the builder loader
	 */
	public BuilderLoader<K,OUT,F>  ttl(Duration duration) {
		return new BuilderLoader<K,OUT,F>(placer,duration.toMillis(),key,value,true);
	}
	
	/**
	 * Creates the.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<F> create() {
		return placer.apply(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuilderLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + "]";
	}
	
}