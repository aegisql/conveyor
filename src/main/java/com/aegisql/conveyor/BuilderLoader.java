package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class BuilderLoader<K,OUT,F> {

	private final Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer;
	public final long creationTime = System.currentTimeMillis(); 
	
	public final long expirationTime;
	public final K key;
	public final BuilderSupplier<OUT> value;
	
	private BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer, long expirationTime, K key, BuilderSupplier<OUT> value) {
		this.placer = placer;
		this.expirationTime = expirationTime;
		this.key = key;
		this.value = value;
	}

	private BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer, long ttl, K key, BuilderSupplier<OUT> value, boolean dumb) {
		this.placer = placer;
		this.expirationTime = creationTime + ttl;
		this.key = key;
		this.value = value;
	}

	BuilderLoader(Function<BuilderLoader<K,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,0,null,null);
	}
	
	public BuilderLoader<K,OUT,F> id(K k) {
		return new BuilderLoader<K,OUT,F>(placer,expirationTime,k,value);
	}

	public BuilderLoader<K,OUT,F> builder(BuilderSupplier<OUT> v) {
		return new BuilderLoader<K,OUT,F>(placer,expirationTime,key,v);
	}

	public BuilderLoader<K,OUT,F>  expirationTime(long et) {
		return new BuilderLoader<K,OUT,F>(placer,et,key,value);
	}
	
	public BuilderLoader<K,OUT,F>  expirationTime(Instant instant) {
		return new BuilderLoader<K,OUT,F>(placer,instant.toEpochMilli(),key,value);
	}
	
	public BuilderLoader<K,OUT,F>  ttl(long time, TimeUnit unit) {
		return new BuilderLoader<K,OUT,F>(placer,TimeUnit.MILLISECONDS.convert(time, unit),key,value ,true);
	}
	
	public BuilderLoader<K,OUT,F>  ttl(Duration duration) {
		return new BuilderLoader<K,OUT,F>(placer,duration.toMillis(),key,value,true);
	}
	
	public CompletableFuture<F> place() {
		return placer.apply(this);
	}

	@Override
	public String toString() {
		return "BuilderLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", key=" + key + "]";
	}
	
}
