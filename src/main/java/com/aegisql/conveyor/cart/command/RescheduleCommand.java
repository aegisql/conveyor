package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class RescheduleCommand.
 *
 * @param <K> the key type
 */
public class RescheduleCommand<K> extends GeneralCommand<K, String> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603062972969708346L;

	/**
	 * Instantiates a new reschedule command.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public RescheduleCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, "RESCHEDULE", CommandLabel.RESCHEDULE_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new reschedule command.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public RescheduleCommand(K k, long expiration) {
		super(k, "RESCHEDULE",  CommandLabel.RESCHEDULE_BUILD, expiration);
	}

}
