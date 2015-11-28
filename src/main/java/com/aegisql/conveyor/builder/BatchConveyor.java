package com.aegisql.conveyor.builder;

import java.util.List;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

public class BatchConveyor <V> extends AssemblingConveyor<String, SmartLabel<BatchCollectingBuilder<V>>, BatchCart<V>, List<V>> {

	public BatchConveyor() {
		super();
		this.setName("BatchConveyor");
		this.setOnTimeoutAction((b)->{
			BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)b;
			builder.setReady(true);
		});
	}

}
