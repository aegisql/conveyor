package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class FutureLoader<K,OUT> {

	private final Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer;
	public final long creationTime = System.currentTimeMillis(); 
	
	public final long expirationTime;
	public final K key;

	private FutureLoader(Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer, long expirationTime, K key) {
		this.placer = placer;
		this.expirationTime = expirationTime;
		this.key = key;
	}

	private FutureLoader(Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer, long ttl, K key, boolean dumb) {
		this.placer = placer;
		this.expirationTime = creationTime + ttl;
		this.key = key;
	}

	public FutureLoader(Function<FutureLoader<K,OUT>, CompletableFuture<OUT>> placer) {
		this(placer,0,null);
	}
	
	public FutureLoader<K,OUT> id(K k) {
		return new FutureLoader<K,OUT>(placer,expirationTime,k);
	}

	public FutureLoader<K,OUT>  expirationTime(long et) {
		return new FutureLoader<K,OUT>(placer,et,key);
	}
	
	public FutureLoader<K,OUT>  expirationTime(Instant instant) {
		return new FutureLoader<K,OUT>(placer,instant.toEpochMilli(),key);
	}
	
	public FutureLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new FutureLoader<K,OUT>(placer,TimeUnit.MILLISECONDS.convert(time, unit),key ,true);
	}
	
	public FutureLoader<K,OUT>  ttl(Duration duration) {
		return new FutureLoader<K,OUT>(placer,duration.toMillis(),key,true);
	}
	
	public CompletableFuture<OUT> get() {
		return placer.apply(this);
	}

	@Override
	public String toString() {
		return "FutureLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", key=" + key + "]";
	}
	
}
