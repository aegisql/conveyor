package com.aegisql.conveyor.cart.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class CreateCommand.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public class CreateCommand<K, OUT> extends GeneralCommand<K, BuilderSupplier<OUT>> implements Supplier<BuilderSupplier<OUT>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603066172969708346L;

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param builderSupplier the builder supplier
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, long ttl, TimeUnit timeUnit) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param builderSupplier the builder supplier
	 * @param expiration the expiration
	 */
	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, long expiration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, expiration);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param builderSupplier the builder supplier
	 * @param duration the duration
	 */
	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, Duration duration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, duration);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param builderSupplier the builder supplier
	 * @param instant the instant
	 */
	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, Instant instant) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, instant);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param builderSupplier the builder supplier
	 */
	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CreateCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public CreateCommand(K k, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, expiration);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param duration the duration
	 */
	public CreateCommand(K k, Duration duration) {
		super(k, null, CommandLabel.CREATE_BUILD, duration);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 * @param instant the instant
	 */
	public CreateCommand(K k, Instant instant) {
		super(k, null, CommandLabel.CREATE_BUILD, instant);
	}

	/**
	 * Instantiates a new creates the command.
	 *
	 * @param k the k
	 */
	public CreateCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public BuilderSupplier<OUT> get() {
		return getValue();
	}

}
