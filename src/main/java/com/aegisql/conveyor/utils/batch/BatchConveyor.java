package com.aegisql.conveyor.utils.batch;

import java.util.List;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.PartLoader;

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
	
	public final SmartLabel<BatchCollectingBuilder<V>> BATCH = SmartLabel.of((b,v)->{
		BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)b;
		BatchCollectingBuilder.add(builder, (V)v);
	} ); 

	@Override
	public <X> PartLoader<String, SmartLabel<BatchCollectingBuilder<V>>, X, List<V>, Boolean> part() {
		return (PartLoader<String, SmartLabel<BatchCollectingBuilder<V>>, X, List<V>, Boolean>) super.part().label(BATCH).id("_BATCH_");
	}


	
}
