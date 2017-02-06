package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.command.GeneralCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class CommandLoader.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public final class MultiKeyCommandLoader<K,L,OUT> {
	
	/** The conveyor. */
	private final Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor;
	
	/** The creation time. */
	public final long creationTime = System.currentTimeMillis(); 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final Predicate<K> filter;
	
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
	private MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long expirationTime, long ttlMsec, Predicate<K> filter, L label) {
		this.conveyor = conveyor;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.filter = filter;
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
	private MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor, long ttl, Predicate<K> filter, L label, boolean dumb) {
		this.conveyor = conveyor;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.filter = filter;
		this.label = label;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 */
	public MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor) {
		this(conveyor,0,0,k -> false,null);
	}
	
	/**
	 * Id.
	 *
	 * @param k the k
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT> foreach(K k) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,expirationTime,ttlMsec,key -> true,label);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT> label(L l) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,expirationTime,ttlMsec,filter,l);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT>  expirationTime(long et) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,et,ttlMsec,filter,label);
	}
	
	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT>  expirationTime(Instant instant) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,instant.toEpochMilli(),ttlMsec,filter,label);
	}
	
	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT>  ttl(long time, TimeUnit unit) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,TimeUnit.MILLISECONDS.convert(time, unit),filter,label,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,L,OUT>  ttl(Duration duration) {
		return new MultiKeyCommandLoader<K,L,OUT>(conveyor,duration.toMillis(),filter,label,true);
	}
	
	/**
	 * Cancel.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"CANCEL",CommandLabel.CANCEL_BUILD,expirationTime));
	}

	/**
	 * Check.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> timeout() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"TIMEOUT",CommandLabel.TIMEOUT_BUILD,expirationTime));
	}

	/**
	 * Reschedule.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> reschedule() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"RESCHEDULE",CommandLabel.RESCHEDULE_BUILD,expirationTime));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MultiKeyCommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", command=" + label + "]";
	}
	
}
