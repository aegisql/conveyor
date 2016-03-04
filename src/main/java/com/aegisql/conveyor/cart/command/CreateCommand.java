package com.aegisql.conveyor.cart.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;

public class CreateCommand<K, OUT> extends AbstractCommand<K, BuilderSupplier<OUT>> implements Supplier<BuilderSupplier<OUT>> {

	private static final long serialVersionUID = 4603066172969708346L;

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, long ttl, TimeUnit timeUnit) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, long expiration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, expiration);
	}

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, Duration duration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, duration);
	}

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier, Instant instant) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, instant);
	}

	public CreateCommand(K k, BuilderSupplier<OUT> builderSupplier) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD);
	}

	public CreateCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	public CreateCommand(K k, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, expiration);
	}

	public CreateCommand(K k, Duration duration) {
		super(k, null, CommandLabel.CREATE_BUILD, duration);
	}

	public CreateCommand(K k, Instant instant) {
		super(k, null, CommandLabel.CREATE_BUILD, instant);
	}

	public CreateCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}
	
	@Override
	public BuilderSupplier<OUT> get() {
		return getValue();
	}

}
