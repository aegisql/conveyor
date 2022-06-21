package com.aegisql.conveyor.utils.delay_line;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.PartLoader;

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

	public SmartLabel<DelayLineBuilder<IN>> DELAY = SmartLabel.of((b,v)->{
		DelayLineBuilder.add((DelayLineBuilder<IN>)b, (IN)v);
	});
	
	@Override
	public PartLoader<K, SmartLabel<DelayLineBuilder<IN>>> part() {
		return (PartLoader<K, SmartLabel<DelayLineBuilder<IN>>>) super.part().label(DELAY);
	}

}
