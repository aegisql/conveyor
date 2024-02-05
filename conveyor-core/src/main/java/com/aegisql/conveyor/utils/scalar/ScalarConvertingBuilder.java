package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.utils.CommonBuilder;

import java.util.concurrent.TimeUnit;

// TODO: Auto-generated Javadoc
/**
 * The Class ScalarConvertingBuilder.
 *
 * @param <T> the generic type
 * @param <OUT> the generic type
 */
public abstract class ScalarConvertingBuilder<T,OUT> extends CommonBuilder<OUT> {
	
	/** The scalar. */
	protected T scalar = null;
	
	/**
	 * Instantiates a new scalar converting builder.
	 *
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public ScalarConvertingBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
	}

	/**
	 * Instantiates a new scalar converting builder.
	 *
	 * @param expiration the expiration
	 */
	public ScalarConvertingBuilder(long expiration ) {
		super(expiration);
	}

	/**
	 * Instantiates a new scalar converting builder.
	 */
	public ScalarConvertingBuilder() {
		super();
	}

	/**
	 * Adds the.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param value the value
	 */
	public static <T> void add(ScalarConvertingBuilder<T,?> builder, T value) {
		builder.scalar = value;
		builder.ready  = true;
	}

	@Override
	public boolean test() {
		return true;
	}

}
