package com.aegisql.conveyor.utils.batch;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

	static class BatchComplete {}
	
	/**
	 * Instantiates a new batch conveyor.
	 */
	public BatchConveyor() {
		super();
		this.setName("BatchConveyor");
	}
	
	public static final BatchComplete COMPLETE = new BatchComplete();
	
	public final SmartLabel<BatchCollectingBuilder<V>> BATCH = SmartLabel.<BatchCollectingBuilder<V>,V>of(BatchCollectingBuilder::add).intercept(Iterable.class, (b, v)->{
		v.forEach(val-> BatchCollectingBuilder.add(b, (V)val));
	}).intercept(BatchComplete.class, BatchCollectingBuilder::complete);

	@Override
	public PartLoader<String, SmartLabel<BatchCollectingBuilder<V>>> part() {
		return super.part().label(BATCH).id("_BATCH_");
	}

	public CompletableFuture<Boolean> completeBatch() {
		return completeBatch("_BATCH_");
	}

	public CompletableFuture<Boolean> completeBatch(String id) {
		return super.part().label(BATCH).id(id).value(COMPLETE).place();
	}

}
