package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.CommandLabel;

public class TimeoutCommand<K, B extends Supplier<?>> extends AbstractCommand<K, Consumer<B>> {

	private static final long serialVersionUID = 4603066172969708346L;

	public TimeoutCommand(K k, Consumer<B> onTimeout, long ttl, TimeUnit timeUnit) {
		super(k, onTimeout, CommandLabel.TIMEOUT_BUILD, ttl, timeUnit);
	}

	public TimeoutCommand(K k, Consumer<B> onTimeout, long expiration) {
		super(k, onTimeout, CommandLabel.TIMEOUT_BUILD, expiration);
	}

	public TimeoutCommand(K k, Consumer<B> onTimeout) {
		super(k, onTimeout, CommandLabel.TIMEOUT_BUILD);
	}

	public TimeoutCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.TIMEOUT_BUILD, ttl, timeUnit);
	}

	public TimeoutCommand(K k, long expiration) {
		super(k, null, CommandLabel.TIMEOUT_BUILD, expiration);
	}

	public TimeoutCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}

}
