package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.CommandLabel;

public class CancelCommand<K> extends AbstractCommand<K, Consumer<Supplier<?>>> {

	private static final long serialVersionUID = 4603066172969708346L;

	public CancelCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CANCEL_BUILD, ttl, timeUnit);
	}

	public CancelCommand(K k, long expiration) {
		super(k, null,  CommandLabel.CANCEL_BUILD, expiration);
	}

	public CancelCommand(K k) {
		super(k, null,  CommandLabel.CANCEL_BUILD);
	}

}
