package com.aegisql.conveyor.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.DelayHolder;
import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;

public abstract class CommonBuilder<T> implements Testing, Expireable, Supplier<T> {
	
	protected final DelayHolder delay;

	protected boolean ready = false;
	
	public CommonBuilder(long ttl, TimeUnit timeUnit ) {
		delay = new DelayHolder(ttl,timeUnit);
	}

	public CommonBuilder(long expiration ) {
		delay = new DelayHolder(expiration);
	}

	public CommonBuilder(Duration duration ) {
		delay = new DelayHolder(duration);
	}

	public CommonBuilder(Instant instant ) {
		delay = new DelayHolder(instant);
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
	
	@Override
	public long getExpirationTime() {
		return delay.getExpirationTime();
	}
	
}
