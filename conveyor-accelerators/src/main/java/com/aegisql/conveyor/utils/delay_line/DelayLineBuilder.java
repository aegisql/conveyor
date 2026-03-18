package com.aegisql.conveyor.utils.delay_line;

import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.utils.CommonBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayLineBuilder.
 *
 * @param <T> the generic type
 */
public class DelayLineBuilder<T> extends CommonBuilder<T> implements TimeoutAction {
	
	/** The scalar. */
	private T scalar;
	
	/**
	 * Instantiates a new delay line builder.
	 */
	public DelayLineBuilder() {
		super();
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public T get() {
		return scalar;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.TimeoutAction#onTimeout()
	 */
	@Override
	public void onTimeout() {
		ready = true;
	}
	
	/**
	 * Adds the.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param val the val
	 */
	public static <T> void add(DelayLineBuilder<T> builder, T val) {
		builder.scalar = val;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.CommonBuilder#test()
	 */
	@Override
	public boolean test() {
		return ready;
	}
	
}
