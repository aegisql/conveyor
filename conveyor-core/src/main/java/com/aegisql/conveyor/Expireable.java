/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import com.aegisql.conveyor.delay.DelayedExpireable;

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
	long getExpirationTime();
	
	/**
	 * addTime.
	 *
	 * @param time the time
	 * @return new instance of Expireable with added time
	 */
	default Expireable addTime(long time) {
		Expireable e = this;
		return () -> e.getExpirationTime() + time;
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
		return isExpireable() && getExpirationTime() <= System.currentTimeMillis();
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
		return new DelayedExpireable(this);
	}
}
