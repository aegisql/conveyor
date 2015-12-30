package com.aegisql.conveyor;

import java.util.concurrent.Delayed;

public interface Expireable extends Delayed {
	public long getExpirationTime();
}
