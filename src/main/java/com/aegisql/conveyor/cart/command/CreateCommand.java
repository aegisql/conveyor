package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.CommandLabel;

public class CreateCommand<K, B extends Supplier<?>> extends AbstractCommand<K, Supplier<B>> {

	private static final long serialVersionUID = 4603066172969708346L;

	public CreateCommand(K k, Supplier<B> builderSupplier, long ttl, TimeUnit timeUnit) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	public CreateCommand(K k, Supplier<B> builderSupplier, long expiration) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD, expiration);
	}

	public CreateCommand(K k, Supplier<B> builderSupplier) {
		super(k, builderSupplier, CommandLabel.CREATE_BUILD);
	}

	public CreateCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CREATE_BUILD, ttl, timeUnit);
	}

	public CreateCommand(K k, long expiration) {
		super(k, null, CommandLabel.CREATE_BUILD, expiration);
	}

	public CreateCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}

}
