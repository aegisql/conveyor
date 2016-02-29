package com.aegisql.conveyor.utils.delay_line;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

public class DelayLineConveyor <K,IN> extends AssemblingConveyor<K, SmartLabel<DelayLineBuilder<IN>>, IN> {

	public DelayLineConveyor() {
		super();
		this.setName("DelayLineConveyor");
		this.setBuilderSupplier(DelayLineBuilder::new);
	}

}
