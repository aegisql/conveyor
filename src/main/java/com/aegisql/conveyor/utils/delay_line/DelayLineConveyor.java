package com.aegisql.conveyor.utils.delay_line;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class DelayLineConveyor.
 *
 * @param <K> the key type
 * @param <IN> the generic type
 */
public class DelayLineConveyor <K,IN> extends AssemblingConveyor<K, SmartLabel<DelayLineBuilder<IN>>, IN> {

	/**
	 * Instantiates a new delay line conveyor.
	 */
	public DelayLineConveyor() {
		super();
		this.setName("DelayLineConveyor");
		this.setBuilderSupplier(DelayLineBuilder::new);
	}

}
