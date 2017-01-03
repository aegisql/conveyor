package com.aegisql.conveyor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

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
	 * Expired.
	 *
	 * @return true, if less then current timestamp
	 */
	default boolean expired() {
		return isExpireable() && getExpirationTime() < System.currentTimeMillis();
	}

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
