package com.aegisql.conveyor.utils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.DelayHolder;
import com.aegisql.conveyor.Testing;

public abstract class CommonBuilder<T> implements Testing, Delayed, Supplier<T> {
	
	protected final DelayHolder delay;

	protected boolean ready = false;
	
	public CommonBuilder(long ttl, TimeUnit timeUnit ) {
		delay = new DelayHolder(ttl,timeUnit);
	}

	public CommonBuilder(long expiration ) {
		delay = new DelayHolder(expiration);
	}

	public CommonBuilder() {
		delay = new DelayHolder();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed o) {
		return delay.compareTo(((CommonBuilder)o).delay);
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return delay.getDelay(unit);
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public boolean test() {
		return ready;
	}
}
