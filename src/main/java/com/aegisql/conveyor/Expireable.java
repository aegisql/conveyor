package com.aegisql.conveyor;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public interface Expireable {
	public long getExpirationTime();

	public static Delayed toDelayed(Expireable e) {
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
