package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;

public class AckCommand<K> extends AbstractCommand<K, String> {

	private static final long serialVersionUID = 4603066172969708346L;

	public AckCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.ACK_BUILD, ttl, timeUnit);
	}

	public AckCommand(K k, long expiration) {
		super(k, null,  CommandLabel.ACK_BUILD, expiration);
	}

	public AckCommand(K k) {
		super(k, null,  CommandLabel.ACK_BUILD);
	}

}
