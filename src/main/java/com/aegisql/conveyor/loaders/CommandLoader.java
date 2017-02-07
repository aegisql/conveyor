package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.cart.command.GeneralCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class CommandLoader.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public final class CommandLoader<K,L,OUT> {
	
	/** The conveyor. */
	private final Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor;
	
	/** The creation time. */
	public final long creationTime = System.currentTimeMillis(); 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final K key;
	
	/** The label. */
	public final L label;
	
	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 * @param expirationTime the expiration time
	 * @param key the key
	 * @param label the label
	 */
	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long expirationTime, long ttlMsec, K key, L label) {
		this.conveyor = conveyor;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
		this.label = label;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 * @param ttl the ttl
	 * @param key the key
	 * @param label the label
	 * @param dumb the dumb
	 */
	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long ttl, K key, L label, boolean dumb) {
		this.conveyor = conveyor;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.key = key;
		this.label = label;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 */
	public CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor) {
		this(conveyor,0,0,null,null);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the command loader
	 */
	public CommandLoader<K,L,OUT> id(K k) {
		return new CommandLoader<K,L,OUT>(conveyor,expirationTime,ttlMsec,k,label);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the command loader
	 */
	public CommandLoader<K,L,OUT>  expirationTime(long et) {
		return new CommandLoader<K,L,OUT>(conveyor,et,ttlMsec,key,label);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public CommandLoader<K,L,OUT>  expirationTime(Instant instant) {
		return new CommandLoader<K,L,OUT>(conveyor,instant.toEpochMilli(),ttlMsec,key,label);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the command loader
	 */
	public CommandLoader<K,L,OUT>  ttl(long time, TimeUnit unit) {
		return new CommandLoader<K,L,OUT>(conveyor,TimeUnit.MILLISECONDS.convert(time, unit),key,label,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the command loader
	 */
	public CommandLoader<K,L,OUT>  ttl(Duration duration) {
		return new CommandLoader<K,L,OUT>(conveyor,duration.toMillis(),key,label,true);
	}
	
	/**
	 * Cancel.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"CANCEL",CommandLabel.CANCEL_BUILD,expirationTime));
	}

	public CompletableFuture<Boolean> timeout() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"TIMEOUT",CommandLabel.TIMEOUT_BUILD,expirationTime));
	}

	/**
	 * Reschedule.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> reschedule() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"RESCHEDULE",CommandLabel.RESCHEDULE_BUILD,expirationTime));
	}
	
	/**
	 * Check.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> check() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"CHECK",CommandLabel.CHECK_BUILD,expirationTime));
	}

	/**
	 * Create.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> create() {
		return conveyor.apply(new CreateCommand<K,OUT>(key,expirationTime));
	}

	/**
	 * Create.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> create(BuilderSupplier<OUT> builder) {
		return conveyor.apply(new CreateCommand<K,OUT>(key,builder,expirationTime));
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + ", command=" + label + "]";
	}
	
}
