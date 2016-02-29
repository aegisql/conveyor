package com.aegisql.conveyor.utils.delay_line;

import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;

public class DelayLineBuilder<T> implements TimeoutAction, Supplier<T>, Testing {
	
	private T scalar;
	private boolean ready = false;
	
	public DelayLineBuilder() {
		super();
	}

	@Override
	public T get() {
		return scalar;
	}

	@Override
	public void onTimeout() {
		ready = true;
	}
	
	public static <T> void add(DelayLineBuilder<T> builder, T val) {
		builder.scalar = val;
	}

	@Override
	public boolean test() {
		return ready;
	}
	
}
