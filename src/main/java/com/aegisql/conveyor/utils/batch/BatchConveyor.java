package com.aegisql.conveyor.utils.batch;

import java.util.List;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class BatchConveyor.
 *
 * @param <V> the value type
 */
public class BatchConveyor <V> extends AssemblingConveyor<String, SmartLabel<BatchCollectingBuilder<V>>, List<V>> {

	/**
	 * Instantiates a new batch conveyor.
	 */
	public BatchConveyor() {
		super();
		this.setName("BatchConveyor");
		this.setOnTimeoutAction((b)->{
			BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)b;
			builder.setReady(true);
		});
	}

}
