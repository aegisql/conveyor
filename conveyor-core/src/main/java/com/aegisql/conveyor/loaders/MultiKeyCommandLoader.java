package com.aegisql.conveyor.loaders;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.command.GeneralCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class CommandLoader.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public final class MultiKeyCommandLoader<K,OUT> {
	
	/** The conveyor. */
	private final Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor;
	
	/** The creation time. */
	public final long creationTime; 
	
	/** The expiration time. */
	public final long expirationTime;

	/** The ttl msec. */
	public final long ttlMsec;

	/** The key. */
	public final Predicate<K> filter;
	
	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 * @param creationTime the creation time
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param filter the filter
	 */
	MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor,long creationTime, long expirationTime, long ttlMsec, Predicate<K> filter) {
		this.conveyor = conveyor;
		this.creationTime = creationTime;
		this.expirationTime = expirationTime;
		this.ttlMsec = ttlMsec;
		this.filter = filter;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 * @param creationTime the creation time
	 * @param ttl the ttl
	 * @param filter the filter
	 * @param dumb the dumb
	 */
	private MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor,long creationTime, long ttl, Predicate<K> filter, boolean dumb) {
		this.conveyor = conveyor;
		this.creationTime = creationTime;
		this.expirationTime = creationTime + ttl;
		this.ttlMsec = ttl;
		this.filter = filter;
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param conveyor the conveyor
	 */
	public MultiKeyCommandLoader(Function<GeneralCommand<K,?>, CompletableFuture<Boolean>> conveyor) {
		this(conveyor,System.currentTimeMillis(),0,0,k -> false);
	}
	
	/**
	 * Id.
	 *
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT> foreach() {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,expirationTime,ttlMsec,key -> true);
	}

	/**
	 * Id.
	 *
	 * @param p the p
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT> foreach(Predicate<K> p) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,expirationTime,ttlMsec,p);
	}

	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  expirationTime(long et) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,et,ttlMsec,filter);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the multi key command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  creationTime(long ct) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,ct,expirationTime,ttlMsec,filter);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  expirationTime(Instant instant) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,instant.toEpochMilli(),ttlMsec,filter);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the multi key command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  creationTime(Instant instant) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,instant.toEpochMilli(),expirationTime,ttlMsec,filter);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,TimeUnit.MILLISECONDS.convert(time, unit),filter,true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the command loader
	 */
	public MultiKeyCommandLoader<K,OUT>  ttl(Duration duration) {
		return new MultiKeyCommandLoader<K,OUT>(conveyor,creationTime,duration.toMillis(),filter,true);
	}
	
	/**
	 * Cancel.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"CANCEL",CommandLabel.CANCEL_BUILD,creationTime,expirationTime));
	}

	/**
	 * Check.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> timeout() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"TIMEOUT",CommandLabel.TIMEOUT_BUILD,creationTime,expirationTime));
	}

	/**
	 * Reschedule.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> reschedule() {
		return conveyor.apply(new GeneralCommand<K,String>(filter,"RESCHEDULE",CommandLabel.RESCHEDULE_BUILD,creationTime,expirationTime));
	}
	
	public CompletableFuture<List<ProductBin<K,OUT>>> peek() {
		CompletableFuture<List<ProductBin<K,OUT>>> f = new CompletableFuture<>();
		List<ProductBin<K,OUT>> list = new LinkedList<>();
		GeneralCommand<K, Consumer<ProductBin<K,OUT>>> command = new GeneralCommand<>(filter,list::add,CommandLabel.PEEK_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		if(cf.isCancelled()) {
			f.cancel(true);
		}
		if(cf.isCompletedExceptionally()) {
			try {
				cf.get();
			} catch (Exception e) {
				f.completeExceptionally(e);
			}
		}
		return cf.thenCompose( res->{
			if(res) {
				f.complete(list);
			} else {
				f.cancel(true);
			}
			return f;
		});
	}

	public CompletableFuture<Boolean> peek(Consumer<ProductBin<K,OUT>> consumer) {
		GeneralCommand<K, Consumer<ProductBin<K,OUT>>> command = new GeneralCommand<>(filter,consumer,CommandLabel.PEEK_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		return cf;
	}

	public CompletableFuture<List<Memento>> memento() {
		CompletableFuture<List<Memento>> f = new CompletableFuture<>();
		List<Memento> list = new LinkedList<>();
		GeneralCommand<K, Consumer<Memento>> command = new GeneralCommand<>(filter,list::add,CommandLabel.MEMENTO_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		if(cf.isCancelled()) {
			f.cancel(true);
		}
		if(cf.isCompletedExceptionally()) {
			try {
				cf.get();
			} catch (Exception e) {
				f.completeExceptionally(e);
			}
		}
		return cf.thenCompose( res->{
			if(res) {
				f.complete(list);
			} else {
				f.cancel(true);
			}
			return f;
		});
	}

	public CompletableFuture<Boolean> memento(Consumer<Memento> memento) {
		GeneralCommand<K, Consumer<Memento>> command = new GeneralCommand<>(filter,memento,CommandLabel.MEMENTO_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		return cf;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MultiKeyCommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + "]";
	}
	
}
