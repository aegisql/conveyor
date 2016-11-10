package com.aegisql.conveyor.cart.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.CommandLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class FutureCommand.
 *
 * @param <K> the key type
 * @param <OUT> the generic type
 */
public class FutureCommand<K, OUT> extends GeneralCommand<K, CompletableFuture<OUT>> implements Supplier<CompletableFuture<OUT>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603012172969708346L;

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param future the future
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public FutureCommand(K k, CompletableFuture<OUT> future, long ttl, TimeUnit timeUnit) {
		super(k, future, CommandLabel.FUTURE_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param future the future
	 * @param expiration the expiration
	 */
	public FutureCommand(K k, CompletableFuture<OUT> future, long expiration) {
		super(k, future, CommandLabel.FUTURE_BUILD, expiration);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param future the future
	 * @param duration the duration
	 */
	public FutureCommand(K k, CompletableFuture<OUT> future, Duration duration) {
		super(k, future, CommandLabel.FUTURE_BUILD, duration);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param future the future
	 * @param instant the instant
	 */
	public FutureCommand(K k, CompletableFuture<OUT> future, Instant instant) {
		super(k, future, CommandLabel.FUTURE_BUILD, instant);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param future the future
	 */
	public FutureCommand(K k, CompletableFuture<OUT> future) {
		super(k, future, CommandLabel.FUTURE_BUILD);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public FutureCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public FutureCommand(K k, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, expiration);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param duration the duration
	 */
	public FutureCommand(K k, Duration duration) {
		super(k, null, CommandLabel.CREATE_BUILD, duration);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 * @param instant the instant
	 */
	public FutureCommand(K k, Instant instant) {
		super(k, null, CommandLabel.CREATE_BUILD, instant);
	}

	/**
	 * Instantiates a new future command.
	 *
	 * @param k the k
	 */
	public FutureCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public CompletableFuture<OUT> get() {
		return getValue();
	}

}
