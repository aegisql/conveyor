package com.aegisql.conveyor.utils.scalar;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.utils.CommonBuilder;

public abstract class ScalarConvertingBuilder<T,OUT> extends CommonBuilder<OUT> {
	
	protected T scalar = null;
	
	public ScalarConvertingBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
	}

	public ScalarConvertingBuilder(long expiration ) {
		super(expiration);
	}

	public ScalarConvertingBuilder() {
		super();
	}

	public static <T> void add(ScalarConvertingBuilder<T,?> builder, T value) {
		builder.scalar = value;
		builder.ready  = true;
	}

}
