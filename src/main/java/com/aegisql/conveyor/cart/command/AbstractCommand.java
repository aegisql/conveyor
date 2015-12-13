package com.aegisql.conveyor.cart.command;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.cart.AbstractCart;

public abstract class AbstractCommand<K, V> extends AbstractCart<K, V, CommandLabel> {

	private static final long serialVersionUID = -5709296056171099659L;

	public AbstractCommand(K k, V v, CommandLabel label, long ttl, TimeUnit timeUnit) {
		super(k, v, label, ttl, timeUnit);
	}

	public AbstractCommand(K k, V v, CommandLabel label, long expiration) {
		super(k, v, label, expiration);
	}

	public AbstractCommand(K k, V v, CommandLabel label) {
		super(k, v, label);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" [key=" + k + 
				", value=" + v + 
				", label=" + label + 
				", delay=" + delay +
				 "]";
	}

	
}
