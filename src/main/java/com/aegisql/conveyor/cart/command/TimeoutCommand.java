package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class TimeoutCommand.
 *
 * @param <K> the key type
 */
public class TimeoutCommand<K> extends GeneralCommand<K, String> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603066172969708346L;

	/**
	 * Instantiates a new timeout command.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public TimeoutCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.TIMEOUT_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new timeout command.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public TimeoutCommand(K k, long expiration) {
		super(k, null, CommandLabel.TIMEOUT_BUILD, expiration);
	}

	/**
	 * Instantiates a new timeout command.
	 *
	 * @param k the k
	 */
	public TimeoutCommand(K k) {
		super(k, null, CommandLabel.TIMEOUT_BUILD);
	}

}
