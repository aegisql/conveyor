package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.aegisql.conveyor.ProductBin;

public final class ResultConsumerLoader<K,OUT> {

	public final K key;
	
	public final Consumer<ProductBin<K, OUT>> consumer;
	
	public final Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;
	
	/** The ttl msec. */
	public final long ttlMsec;
	
	private final Consumer<Consumer<ProductBin<K,OUT>>> globalPlacer;
	
	public final Predicate<K> filter;
	
	public ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer,
			Consumer<Consumer<ProductBin<K,OUT>>> globalPlacer,
			Consumer<ProductBin<K, OUT>> consumer
			) {
		this(placer,globalPlacer,System.currentTimeMillis(),0,0,null,consumer,null);
	}

	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<Consumer<ProductBin<K,OUT>>> globalPlacer,
			long creationTime,
			long expirationTime,
			long ttlMsec,
			K key, 
			Consumer<ProductBin<K, OUT> > consumer,
			Predicate<K> filter ) {
		this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer;
		this.key            = key;
		this.creationTime   = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec        = ttlMsec;
		this.filter         = filter;
	}

	private ResultConsumerLoader(
			Function<ResultConsumerLoader<K,OUT>,CompletableFuture<Boolean>> placer, 
			Consumer<Consumer<ProductBin<K,OUT>>> globalPlacer,
			long creationTime,
			long ttlMsec,
			K key, 
			Consumer<ProductBin<K, OUT> > consumer,
			Predicate<K> filter,
			boolean dumb) {
		this.placer         = placer;
		this.globalPlacer   = globalPlacer;
		this.consumer       = consumer;
		this.key            = key;
		this.creationTime   = creationTime;
		this.expirationTime = creationTime + ttlMsec;
		this.ttlMsec        = ttlMsec;
		this.filter         = filter;
	}

	public ResultConsumerLoader<K,OUT> id(K k) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				k,
				this.consumer,
				null/*either key or filter*/);
	}

	public ResultConsumerLoader<K,OUT> foreach(Predicate<K> f) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				null/*either key or filter*/,
				this.consumer,
				f);
	}

	public ResultConsumerLoader<K,OUT> foreach() {
		return foreach(k->true);
	}

	public ResultConsumerLoader<K,OUT> first(Consumer<ProductBin<K, OUT> > consumer) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				this.key,
				consumer,filter);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(long et) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,et,0,key,consumer,filter);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  expirationTime(Instant instant) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,instant.toEpochMilli(),0,key,consumer,filter);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer, creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,consumer,filter,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the part loader
	 */
	public ResultConsumerLoader<K,OUT>  ttl(Duration duration) {
		return new ResultConsumerLoader<K,OUT>(placer,globalPlacer,creationTime,duration.toMillis(),key,consumer,filter,true);
	}

	
	public ResultConsumerLoader<K,OUT> andThen(Consumer<ProductBin<K, OUT> > consumer) {
		return new ResultConsumerLoader<>(
				this.placer,
				this.globalPlacer,
				this.creationTime,
				this.expirationTime,
				this.ttlMsec,
				this.key,
				this.consumer != null ? this.consumer.andThen(consumer) : consumer
				,this.filter
				);
	}

	public CompletableFuture<Boolean> set() {
		if(key == null && filter == null) {
			CompletableFuture<Boolean> ready = new CompletableFuture<>();
			ready.complete(true);
			globalPlacer.accept(consumer);
			return ready;
		} else {
			return placer.apply(this);
		}
	}

	@Override
	public String toString() {
		return "ResultConsumerLoader [" + (key==null?"default":"key="+key) + ", creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + "]";
	}
	
	
	
}
