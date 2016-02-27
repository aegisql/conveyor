package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.aegisql.conveyor.CommandLabel;

public class RescheduleCommand<K,B> extends AbstractCommand<K, BiConsumer<B,B>> {

	private static final long serialVersionUID = 4603062972969708346L;

	public RescheduleCommand(K k, BiConsumer<B, B> consumer, long ttl, TimeUnit timeUnit) {
		super(k, consumer, CommandLabel.RESCHEDULE_BUILD, ttl, timeUnit);
	}

	public RescheduleCommand(K k, BiConsumer<B, B> consumer, long expiration) {
		super(k, consumer,  CommandLabel.RESCHEDULE_BUILD, expiration);
	}

}
