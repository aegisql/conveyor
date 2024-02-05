package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CommonBuilder.
 *
 * @param <T> the generic type
 */
public abstract class CommonBuilder<T> implements Testing, Expireable, Supplier<T> {
	
	/** The creation time. */
	protected final long creationTime = System.currentTimeMillis(); 

	/** The expiration time. */
	protected final long expirationTime; 

	/** The ready. */
	protected boolean ready = false;
	
	/**
	 * Instantiates a new common builder.
	 *
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CommonBuilder(long ttl, TimeUnit timeUnit ) {
		this.expirationTime = creationTime + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	/**
	 * Instantiates a new common builder.
	 *
	 * @param expiration the expiration
	 */
	public CommonBuilder(long expiration ) {
		this.expirationTime = expiration;
	}

	/**
	 * Instantiates a new common builder.
	 *
	 * @param duration the duration
	 */
	public CommonBuilder(Duration duration ) {
		this.expirationTime = creationTime + duration.toMillis();
	}

	/**
	 * Instantiates a new common builder.
	 *
	 * @param instant the instant
	 */
	public CommonBuilder(Instant instant ) {
		this.expirationTime = instant.toEpochMilli();
	}

	/**
	 * Instantiates a new common builder.
	 */
	public CommonBuilder() {
		this.expirationTime = 0;
	}

	/**
	 * Sets the ready.
	 *
	 * @param ready the new ready
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Testing#test()
	 */
	@Override
	public boolean test() {
		return ready;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Expireable#getExpirationTime()
	 */
	@Override
	public long getExpirationTime() {
		return expirationTime;
	}
	
}
