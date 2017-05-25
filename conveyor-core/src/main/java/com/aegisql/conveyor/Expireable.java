/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Interface Expireable.
 */
public interface Expireable {
	
	/**
	 * Gets the expiration time.
	 *
	 * @return the expiration time
	 */
	public long getExpirationTime();
	
	/**
	 * addTime.
	 *
	 * @param time the time
	 * @return new instance of Expireable with added time
	 */
	default Expireable addTime(long time) {
		Expireable e = this;
		return new Expireable() {
			@Override
			public long getExpirationTime() {
				return e.getExpirationTime() + time;
			}
		};
	}

	/**
	 * addTime.
	 *
	 * @param time the time
	 * @param unit the unit
	 * @return new instance of Expireable with added time
	 */
	default Expireable addTime(long time, TimeUnit unit) {
		return addTime(unit.toMillis(time));
	}

	/**
	 * addTime.
	 *
	 * @param time the time
	 * @return new instance of Expireable with added time
	 */
	default Expireable addTime(Duration time) {
		return addTime(time.toMillis());
	}
	
	/**
	 * Expired.
	 *
	 * @return true, if less then current timestamp
	 */
	default boolean expired() {
		return isExpireable() && getExpirationTime() < System.currentTimeMillis();
	}

	/**
	 * Checks if is expireable.
	 *
	 * @return true, if is expireable
	 */
	default boolean isExpireable() {
		return getExpirationTime() > 0;
	}

	/**
	 * To delayed.
	 *
	 * @return the delayed
	 */
	default Delayed toDelayed() {
		Expireable e = this;
		return new Delayed() {
			
			final long expirationTime = e.getExpirationTime();

			@Override
			public int compareTo(Delayed o) {
				if (o == this) {
					return 0;
				}
				if (expirationTime < ((Expireable) o).getExpirationTime()) {
					return -1;
				}
				if (expirationTime > ((Expireable) o).getExpirationTime()) {
					return +1;
				}
				return 0;
			}

			@Override
			public long getDelay(TimeUnit unit) {
		        long delta;
				if( expirationTime <= 0 ) {
					delta = Long.MAX_VALUE;
				} else {
					delta = expirationTime - System.currentTimeMillis();
				}
		        return unit.convert(delta, TimeUnit.MILLISECONDS);
			}
		};
	}
}