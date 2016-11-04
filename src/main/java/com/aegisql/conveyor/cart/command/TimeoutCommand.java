package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;

public class TimeoutCommand<K> extends GeneralCommand<K, String> {

	private static final long serialVersionUID = 4603066172969708346L;

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
