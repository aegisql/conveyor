package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.Cart;

public class CancelCommand<K> extends GeneralCommand<K, String> {

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

	@Override
	public Cart<K, String, CommandLabel> copy() {
		return new CancelCommand<K>(getKey(),getExpirationTime());
	}

}
