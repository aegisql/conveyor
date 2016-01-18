package com.aegisql.conveyor.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;

public abstract class CommonBuilder<T> implements Testing, Expireable, Supplier<T> {
	
	protected final long creationTime = System.currentTimeMillis(); 

	protected final long expirationTime; 

	protected boolean ready = false;
	
	public CommonBuilder(long ttl, TimeUnit timeUnit ) {
		this.expirationTime = creationTime + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	public CommonBuilder(long expiration ) {
		this.expirationTime = expiration;
	}

	public CommonBuilder(Duration duration ) {
		this.expirationTime = creationTime + duration.toMillis();
	}

	public CommonBuilder(Instant instant ) {
		this.expirationTime = instant.toEpochMilli();
	}

	public CommonBuilder() {
		this.expirationTime = 0;
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
		return expirationTime;
	}
	
}
