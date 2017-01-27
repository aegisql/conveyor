package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.aegisql.conveyor.cart.command.CancelCommand;
import com.aegisql.conveyor.cart.command.CheckBuildCommand;
import com.aegisql.conveyor.cart.command.GeneralCommand;

public final class CommandLoader<K,L,OUT> {
	
	private final Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor;
	public final long creationTime = System.currentTimeMillis(); 
	
	public final long expirationTime;
	public final K key;
	public final L label;
	
	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long expirationTime, K key, L label) {
		this.conveyor = conveyor;
		this.expirationTime = expirationTime;
		this.key = key;
		this.label = label;
	}

	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long ttl, K key, L label, boolean dumb) {
		this.conveyor = conveyor;
		this.expirationTime = creationTime + ttl;
		this.key = key;
		this.label = label;
	}

	public CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor) {
		this(conveyor,0,null,null);
	}
	
	public CommandLoader<K,L,OUT> id(K k) {
		return new CommandLoader<K,L,OUT>(conveyor,expirationTime,k,label);
	}

	public CommandLoader<K,L,OUT> label(L l) {
		return new CommandLoader<K,L,OUT>(conveyor,expirationTime,key,l);
	}

	public CommandLoader<K,L,OUT>  expirationTime(long et) {
		return new CommandLoader<K,L,OUT>(conveyor,et,key,label);
	}
	
	public CommandLoader<K,L,OUT>  expirationTime(Instant instant) {
		return new CommandLoader<K,L,OUT>(conveyor,instant.toEpochMilli(),key,label);
	}
	
	public CommandLoader<K,L,OUT>  ttl(long time, TimeUnit unit) {
		return new CommandLoader<K,L,OUT>(conveyor,TimeUnit.MILLISECONDS.convert(time, unit),key,label ,true);
	}
	
	public CommandLoader<K,L,OUT>  ttl(Duration duration) {
		return new CommandLoader<K,L,OUT>(conveyor,duration.toMillis(),key,label,true);
	}
	
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new CancelCommand<K>(key,expirationTime));
	}

	public CompletableFuture<Boolean> check() {
		return conveyor.apply(new CheckBuildCommand<K>(key));//TODO: where is expiration time?
	}

	@Override
	public String toString() {
		return "CommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", key=" + key + ", command=" + label + "]";
	}
	
}
