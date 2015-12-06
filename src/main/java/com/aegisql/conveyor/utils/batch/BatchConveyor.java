package com.aegisql.conveyor.utils.batch;

import java.util.List;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;

public class BatchConveyor <V> extends AssemblingConveyor<String, SmartLabel<BatchCollectingBuilder<V>>, Cart<String, V, SmartLabel<BatchCollectingBuilder<V>>>, List<V>> {

	public BatchConveyor() {
		super();
		this.setName("BatchConveyor");
		this.setOnTimeoutAction((b)->{
			BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)b;
			builder.setReady(true);
		});
	}

}
