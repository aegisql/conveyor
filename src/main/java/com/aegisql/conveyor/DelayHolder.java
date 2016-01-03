package com.aegisql.conveyor;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayHolder implements Expireable {
	
	private final long created = System.currentTimeMillis();
	private final long expiration;

	public DelayHolder() {
		this(0);
	}

	public DelayHolder(long expiration) {
		this.expiration = expiration;
	}

	public DelayHolder(long ttl, TimeUnit timeUnit) {
		this.expiration = created + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		if( this.expiration < ((Expireable)o).getExpirationTime()) {
			return -1;
		}
		if( this.expiration > ((Expireable)o).getExpirationTime()) {
			return +1;
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( this.expiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = this.expiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}

	public long getCreatedTime() {
		return created;
	}

	@Override
	public long getExpirationTime() {
		return expiration;
	}

	public boolean expired() {
		return expiration > 0 && expiration <= System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "DelayHolder [created=" + new Date(created) + ", expiration=" + (expiration > 0 ? (", expires=" + new Date(expiration) ) : ", unexpireable") + "]";
	}
	
}
