package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class CancelCommand.
 *
 * @param <K> the key type
 */
public class CancelCommand<K> extends GeneralCommand<K, String> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4603066172969708346L;

	/**
	 * Instantiates a new cancel command.
	 *
	 * @param k the k
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CancelCommand(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CommandLabel.CANCEL_BUILD, ttl, timeUnit);
	}

	/**
	 * Instantiates a new cancel command.
	 *
	 * @param k the k
	 * @param expiration the expiration
	 */
	public CancelCommand(K k, long expiration) {
		super(k, null,  CommandLabel.CANCEL_BUILD, expiration);
	}

	/**
	 * Instantiates a new cancel command.
	 *
	 * @param k the k
	 */
	public CancelCommand(K k) {
		super(k, null,  CommandLabel.CANCEL_BUILD);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.command.GeneralCommand#copy()
	 */
	@Override
	public Cart<K, String, CommandLabel> copy() {
		return new CancelCommand<K>(getKey(),getExpirationTime());
	}

}
