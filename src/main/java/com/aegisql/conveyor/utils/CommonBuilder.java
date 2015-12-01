package com.aegisql.conveyor.utils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;

public abstract class CommonBuilder<T> implements Testing, Delayed, Supplier<T> {
	
	protected final long builderCreated = System.currentTimeMillis();
	protected final long builderExpiration;

	protected boolean ready = false;
	
	public CommonBuilder(long ttl, TimeUnit timeUnit ) {
		this.builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	public CommonBuilder(long expiration ) {
		this.builderExpiration = expiration;
	}

	public CommonBuilder() {
		this.builderExpiration = 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed o) {
		return (int) (builderCreated - ((CommonBuilder)o).builderCreated);
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( builderExpiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = builderExpiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public boolean test() {
		return ready;
	}
}
