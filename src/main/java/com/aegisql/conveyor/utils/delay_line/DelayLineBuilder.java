package com.aegisql.conveyor.utils.delay_line;

import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.utils.CommonBuilder;

public class DelayLineBuilder<T> extends CommonBuilder<T> implements TimeoutAction {
	
	private T scalar;
	
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
