package com.aegisql.conveyor.builder;

import java.util.List;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

public class BatchConveyor <V> extends AssemblingConveyor<String, SmartLabel<BatchCollectingBuilder<V>>, BatchCart<V>, List<V>> {

	public BatchConveyor() {
		super();
		this.setName("BatchConveyor");
		this.setOnTimeoutAction(true);
		this.setCartConsumer((a,b,c)->{
			BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)c;
			builder.setReady(true);
		});
	}

}
