package com.aegisql.conveyor.loaders;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.BuildingSite.Memento;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.command.CreateCommand;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.serial.SerializablePredicate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
	 * @param creation the creation
	 * @param expirationTime the expiration time
	 * @param ttlMsec the ttl msec
	 * @param key the key
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
	 * @param creation the creation
	 * @param ttl the ttl
	 * @param key the key
	 * @param dumb the dumb
	 */
	@SuppressWarnings("SameParameterValue")
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
		return new CommandLoader<>(conveyor, creationTime, expirationTime, ttlMsec, k);
	}

	/**
	 * Foreach.
	 *
	 * @param filter the filter
	 * @return the multi key command loader
	 */
	public MultiKeyCommandLoader<K,OUT> foreach(Predicate<K> filter) {
		return new MultiKeyCommandLoader<>(conveyor, creationTime, expirationTime, ttlMsec, filter);
	}

	/**
	 * Foreach.
	 *
	 * @return the multi key command loader
	 */
	public MultiKeyCommandLoader<K,OUT> foreach() {
		return foreach(SerializablePredicate.ANY);
	}

	
	/**
	 * Expiration time.
	 *
	 * @param et the et
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  expirationTime(long et) {
		return new CommandLoader<>(conveyor, creationTime, et, ttlMsec, key);
	}

	/**
	 * Creation time.
	 *
	 * @param ct the ct
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  creationTime(long ct) {
		return new CommandLoader<>(conveyor, ct, expirationTime, ttlMsec, key);
	}

	/**
	 * Expiration time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  expirationTime(Instant instant) {
		return new CommandLoader<>(conveyor, creationTime, instant.toEpochMilli(), ttlMsec, key);
	}

	/**
	 * Creation time.
	 *
	 * @param instant the instant
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  creationTime(Instant instant) {
		return new CommandLoader<>(conveyor, instant.toEpochMilli(), expirationTime, ttlMsec, key);
	}

	/**
	 * Ttl.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  ttl(long time, TimeUnit unit) {
		return new CommandLoader<>(conveyor, creationTime, TimeUnit.MILLISECONDS.convert(time, unit), key, true);
	}
	
	/**
	 * Ttl.
	 *
	 * @param duration the duration
	 * @return the command loader
	 */
	public CommandLoader<K,OUT>  ttl(Duration duration) {
		return new CommandLoader<>(conveyor, creationTime, duration.toMillis(), key, true);
	}
	
	/**
	 * Cancel.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> cancel() {
		return conveyor.apply(new GeneralCommand<>(key, "CANCEL", CommandLabel.CANCEL_BUILD, creationTime, expirationTime));
	}

	public CompletableFuture<Boolean> complete(OUT result) {
		return conveyor.apply(new GeneralCommand<>(key, result, CommandLabel.COMPLETE_BUILD, creationTime, expirationTime));
	}

	public CompletableFuture<Boolean> completeExceptionally(Throwable error) {
		return conveyor.apply(new GeneralCommand<>(key, error, CommandLabel.COMPLETE_BUILD_EXEPTIONALLY, creationTime, expirationTime));
	}

	public CompletableFuture<Boolean> addProperty(String property, Object value) {
		GeneralCommand<K, Object> command = new GeneralCommand<>(key, null, CommandLabel.PROPERTIES, creationTime, expirationTime);
		command.addProperty(property, value);
		return conveyor.apply(command);
	}

	public CompletableFuture<Boolean> addProperties(Map<String,Object> properties) {
		GeneralCommand<K, Object> command = new GeneralCommand<>(key, null, CommandLabel.PROPERTIES, creationTime, expirationTime);
		properties.forEach(command::addProperty);
		return conveyor.apply(command);
	}

	/**
	 * Timeout.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> timeout() {
		return conveyor.apply(new GeneralCommand<>(key, "TIMEOUT", CommandLabel.TIMEOUT_BUILD, creationTime, expirationTime));
	}

	/**
	 * Reschedule.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> reschedule() {
		return conveyor.apply(new GeneralCommand<>(key, "RESCHEDULE", CommandLabel.RESCHEDULE_BUILD, creationTime, expirationTime));
	}
	
	/**
	 * Check.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> check() {
		return conveyor.apply(new GeneralCommand<>(key, "CHECK", CommandLabel.CHECK_BUILD, creationTime, expirationTime));
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
		return conveyor.apply(new CreateCommand<>(key, builder, creationTime, expirationTime));
	}

	/**
	 * Peek.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<ProductBin<K,OUT>> peek() {
		CompletableFuture<ProductBin<K,OUT>> f = new CompletableFuture<>();
		GeneralCommand<K, Consumer<ProductBin<K,OUT>>> command = new GeneralCommand<>(key,f::complete,CommandLabel.PEEK_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		return cf.thenCompose( res->{
			if( ! res) {
				f.cancel(true);
			}
			return f;
		});
	}

	/**
	 * Peek.
	 *
	 * @param consumer the consumer
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> peek(Consumer<ProductBin<K,OUT>> consumer) {
		GeneralCommand<K, Consumer<ProductBin<K,OUT>>> command = new GeneralCommand<>(key,consumer,CommandLabel.PEEK_BUILD,creationTime,expirationTime);
		return conveyor.apply(command);
	}

	/**
	 * Memento.
	 *
	 * @param consumer the consumer
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> memento(Consumer<Memento> consumer) {
		GeneralCommand<K, Consumer<Memento>> command = new GeneralCommand<>(key,consumer,CommandLabel.MEMENTO_BUILD,creationTime,expirationTime);
		return conveyor.apply(command);
	}

	/**
	 * Memento.
	 *
	 * @return the completable future
	 */
	public CompletableFuture<Memento> memento() {
		CompletableFuture<Memento> f = new CompletableFuture<>();
		GeneralCommand<K, Consumer<Memento>> command = new GeneralCommand<>(key,f::complete,CommandLabel.MEMENTO_BUILD,creationTime,expirationTime);
		CompletableFuture<Boolean> cf = conveyor.apply(command);
		return cf.thenCompose( res->{
			if( ! res) {
				f.cancel(true);
			}
			return f;
		});
	}

	/**
	 * Restore.
	 *
	 * @param memento the memento
	 * @return the completable future
	 */
	public CompletableFuture<Boolean> restore(Memento memento) {
		K id = key;
		if(id==null) {
			id = (K) memento.getId();
		}
		GeneralCommand<K, Memento> command = new GeneralCommand<>(id,memento,CommandLabel.RESTORE_BUILD,creationTime,expirationTime);
		return conveyor.apply(command);
	}

	public CompletableFuture<Boolean> suspend() {
		GeneralCommand<K, Boolean> command = new GeneralCommand<>(x->true,true,CommandLabel.SUSPEND,creationTime,expirationTime);
		return conveyor.apply(command);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CommandLoader [creationTime=" + creationTime + ", expirationTime="
				+ expirationTime + ", ttlMsec=" + ttlMsec + ", key=" + key + "]";
	}
	
	public static <K,OUT> CommandLoader<K,OUT> byConveyorName(String name) {
		return Conveyor.byName(name).command();
	}
	
	public static <K,OUT> Supplier<CommandLoader<K,OUT>> lazySupplier(String name) {
		return new Supplier<>() {
			CommandLoader<K, OUT> command;

			@Override
			public CommandLoader<K, OUT> get() {
				if (command == null) {
					Conveyor<K, ?, OUT> c = Conveyor.byName(name);
					if (c != null) {
						command = c.command();
					}
				}
				return command;
			}
		};
	}
	
}
