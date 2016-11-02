package com.aegisql.conveyor.cart.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.CommandLabel;

public class FutureCommand<K, OUT> extends AbstractCommand<K, CompletableFuture<OUT>> implements Supplier<CompletableFuture<OUT>> {

	private static final long serialVersionUID = 4603012172969708346L;

	public FutureCommand(K k, CompletableFuture<OUT> future, long ttl, TimeUnit timeUnit) {
		super(k, future, CommandLabel.FUTURE_BUILD, ttl, timeUnit);
	}

	public FutureCommand(K k, CompletableFuture<OUT> future, long expiration) {
		super(k, future, CommandLabel.FUTURE_BUILD, expiration);
	}

	public FutureCommand(K k, CompletableFuture<OUT> future, Duration duration) {
		super(k, future, CommandLabel.FUTURE_BUILD, duration);
	}

	public FutureCommand(K k, CompletableFuture<OUT> future, Instant instant) {
		super(k, future, CommandLabel.FUTURE_BUILD, instant);
	}

	public FutureCommand(K k, CompletableFuture<OUT> future) {
		super(k, future, CommandLabel.FUTURE_BUILD);
	}

	public FutureCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	public FutureCommand(K k, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, expiration);
	}

	public FutureCommand(K k, Duration duration) {
		super(k, null, CommandLabel.CREATE_BUILD, duration);
	}

	public FutureCommand(K k, Instant instant) {
		super(k, null, CommandLabel.CREATE_BUILD, instant);
	}

	public FutureCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}
	
	@Override
	public CompletableFuture<OUT> get() {
		return getValue();
	}

}
