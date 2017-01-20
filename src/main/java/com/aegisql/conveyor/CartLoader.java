package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class CartLoader<K,L,V,OUT,F> {

	private final Function<CartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer;
	public final long creationTime = System.currentTimeMillis(); 
	
	public final long expirationTime;
	public final K key;
	public final L label;
	public final V value;
	
	private CartLoader(Function<CartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long expirationTime, K key, L label, V value) {
		this.placer = placer;
		this.expirationTime = expirationTime;
		this.key = key;
		this.label = label;
		this.value = value;
	}

	private CartLoader(Function<CartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer, long ttl, K key, L label, V value, boolean dumb) {
		this.placer = placer;
		this.expirationTime = creationTime + ttl;
		this.key = key;
		this.label = label;
		this.value = value;
	}

	CartLoader(Function<CartLoader<K,L,?,OUT,F>, CompletableFuture<F>> placer) {
		this(placer,0,null,null,null);
	}
	
	public CartLoader<K,L,V,OUT,F> id(K k) {
		return new CartLoader<K,L,V,OUT,F>(placer,expirationTime,k,label,value);
	}

	public CartLoader<K,L,V,OUT,F> label(L l) {
		return new CartLoader<K,L,V,OUT,F>(placer,expirationTime,key,l,value);
	}

	public<X> CartLoader<K,L,X,OUT,F> part(X v) {
		return new CartLoader<K,L,X,OUT,F>(placer,expirationTime,key,label,v);
	}

	public CartLoader<K,L,V,OUT,F>  expirationTime(long et) {
		return new CartLoader<K,L,V,OUT,F>(placer,et,key,label,value);
	}
	
	public CartLoader<K,L,V,OUT,F>  expirationTime(Instant instant) {
		return new CartLoader<K,L,V,OUT,F>(placer,instant.toEpochMilli(),key,label,value);
	}
	
	public CartLoader<K,L,V,OUT,F>  ttl(long time, TimeUnit unit) {
		return new CartLoader<K,L,V,OUT,F>(placer,TimeUnit.MILLISECONDS.convert(time, unit),key,label,value ,true);
	}
	
	public CartLoader<K,L,V,OUT,F>  ttl(Duration duration) {
		return new CartLoader<K,L,V,OUT,F>(placer,duration.toMillis(),key,label,value,true);
	}
	
	public CompletableFuture<F> place() {
		return placer.apply(this);
	}

	@Override
	public String toString() {
		return "CartLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", key=" + key + ", label=" + label + ", value=" + value + "]";
	}
	
}
