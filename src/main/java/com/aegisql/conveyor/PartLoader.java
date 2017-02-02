package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class PartLoader<K,L,V,OUT,F> {

	private final Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer;
	public final long creationTime = System.currentTimeMillis(); 
	
	public final long expirationTime;
	public final long ttlMsec;
	public final K key;
	public final L label;
	public final V partValue;
	
	private PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long expirationTime, long ttlMsec, K key, L label, V value) {
		this.placer = placer;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.label = label;
		this.partValue = value;
	}

	private PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long ttl, K key, L label, V value, boolean dumb) {
		this.placer = placer;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.key = key;
		this.label = label;
		this.partValue = value;
	}

	public PartLoader(Function<PartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,0,0,null,null,null);
	}
	
	public PartLoader<K,L,V,OUT,F> id(K k) {
		return new PartLoader<K,L,V,OUT,F>(placer,expirationTime,ttlMsec,k,label,partValue);
	}

	public PartLoader<K,L,V,OUT,F> label(L l) {
		return new PartLoader<K,L,V,OUT,F>(placer,expirationTime,ttlMsec,key,l,partValue);
	}

	public<X> PartLoader<K,L,X,OUT,F> value(X v) {
		return new PartLoader<K,L,X,OUT,F>(placer,expirationTime,ttlMsec,key,label,v);
	}

	public PartLoader<K,L,V,OUT,F>  expirationTime(long et) {
		return new PartLoader<K,L,V,OUT,F>(placer,et,0,key,label,partValue);
	}
	
	public PartLoader<K,L,V,OUT,F>  expirationTime(Instant instant) {
		return new PartLoader<K,L,V,OUT,F>(placer,instant.toEpochMilli(),0,key,label,partValue);
	}
	
	public PartLoader<K,L,V,OUT,F>  ttl(long time, TimeUnit unit) {
		return new PartLoader<K,L,V,OUT,F>(placer,TimeUnit.MILLISECONDS.convert(time, unit),key,label,partValue ,true);
	}
	
	public PartLoader<K,L,V,OUT,F>  ttl(Duration duration) {
		return new PartLoader<K,L,V,OUT,F>(placer,duration.toMillis(),key,label,partValue,true);
	}
	
	public CompletableFuture<F> place() {
		return placer.apply(this);
	}

	@Override
	public String toString() {
		return "PartLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + ", label=" + label + ", partValue=" + partValue + "]";
	}
	
}
