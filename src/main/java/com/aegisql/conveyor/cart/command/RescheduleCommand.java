package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;

public class RescheduleCommand<K> extends AbstractCommand<K, String> {

	private static final long serialVersionUID = 4603062972969708346L;

	public RescheduleCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, "RESCHEDULE", CommandLabel.RESCHEDULE_BUILD, ttl, timeUnit);
	}

	public RescheduleCommand(K k, long expiration) {
		super(k, "RESCHEDULE",  CommandLabel.RESCHEDULE_BUILD, expiration);
	}

}
