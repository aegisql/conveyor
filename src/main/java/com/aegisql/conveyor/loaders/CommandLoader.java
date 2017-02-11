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
 * @param <OUT> the generic type
 */
public final class CommandLoader<K,OUT> {
	
	/** The conveyor. */
	private final Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final K key;
	
	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 * @param expirationTime the expiration time
	 * @param key the key
	 * @param label the label
	 */
	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long creation, long expirationTime, long ttlMsec, K key) {
		this.conveyor = conveyor;
		this.creationTime = creation;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.key = key;
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
	private CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long creation, long ttl, K key, boolean dumb) {
		this.conveyor = conveyor;
		this.creationTime = creation;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.key = key;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 */
	public CommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor) {
		this(conveyor,System.currentTimeMillis(),0,0,null);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the command loader
	 */
	public CommandLoader<K,OUT> id(K k) {
		return new CommandLoader<K,OUT>(conveyor,creationTime,expirationTime,ttlMsec,k);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  expirationTime(long et) {
		return new CommandLoader<K,OUT>(conveyor,creationTime,et,ttlMsec,key);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  expirationTime(Instant instant) {
		return new CommandLoader<K,OUT>(conveyor,creationTime,instant.toEpochMilli(),ttlMsec,key);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new CommandLoader<K,OUT>(conveyor,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),key,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  ttl(Duration duration) {
		return new CommandLoader<K,OUT>(conveyor,creationTime,duration.toMillis(),key,true);
	}
	
	/**
	 * Cancel.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"CANCEL",CommandLabel.CANCEL_BUILD,creationTime,expirationTime));
	}

	public CompletableFuture<Boolean> timeout() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"TIMEOUT",CommandLabel.TIMEOUT_BUILD,creationTime,expirationTime));
	}

	/**
	 * Reschedule.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> reschedule() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"RESCHEDULE",CommandLabel.RESCHEDULE_BUILD,creationTime,expirationTime));
	}
	
	/**
	 * Check.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> check() {
		return conveyor.apply(new GeneralCommand<K,String>(key,"CHECK",CommandLabel.CHECK_BUILD,creationTime,expirationTime));
	}

	/**
	 * Create.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> create() {
		return conveyor.apply(new CreateCommand<K,OUT>(key,creationTime,expirationTime));
	}

	/**
	 * Create.
	 *
	 * @param builder the builder supplier
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> create(BuilderSupplier<OUT> builder) {
		return conveyor.apply(new CreateCommand<K,OUT>(key,builder,creationTime,expirationTime));
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + "]";
	}
	
}
