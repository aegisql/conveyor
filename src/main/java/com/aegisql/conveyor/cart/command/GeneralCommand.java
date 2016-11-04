package com.aegisql.conveyor.cart.command;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;

public class GeneralCommand<K, V> extends AbstractCart<K, V, CommandLabel> {

	private static final long serialVersionUID = -5709296056171099659L;

	public GeneralCommand(K k, V v, CommandLabel label, long ttl, TimeUnit timeUnit) {
		super(k, v, label, ttl, timeUnit);
	}

	public GeneralCommand(K k, V v, CommandLabel label, long expiration) {
		super(k, v, label, expiration);
	}

	public GeneralCommand(K k, V v, CommandLabel label) {
		super(k, v, label);
	}
	
	public GeneralCommand(K k, V v, CommandLabel label, Duration duration) {
		super(k, v, label, duration);
	}
	
	public GeneralCommand(K k, V v, CommandLabel label, Instant instant) {
		super(k, v, label, instant);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", expirationTime=" + expirationTime +
				 "]";
	}

	@Override
	public Cart<K, V, CommandLabel> copy() {
		return new GeneralCommand<K, V>(getKey(), getValue(), getLabel(),getExpirationTime());
	}

	
}
