package com.aegisql.conveyor.utils.batch;

import java.util.Collection;
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
		this.setOnTimeoutAction((b)->{
			BatchCollectingBuilder<V> builder = (BatchCollectingBuilder<V>)b;
			builder.setReady(true);
		});
	}
	
	public static final BatchComplete COMPLETE = new BatchComplete();
	
	public final SmartLabel<BatchCollectingBuilder<V>> BATCH = SmartLabel.<BatchCollectingBuilder<V>,V>of((b,v)->{
		BatchCollectingBuilder.add(b, (V)v);
	} ).intercept(Iterable.class, (b,v)->{
		v.forEach(val->{
			BatchCollectingBuilder.add(b, (V)val);
		});
	}).intercept(BatchComplete.class, (b,v) -> BatchCollectingBuilder.complete(b, v) );

	@Override
	public PartLoader<String, SmartLabel<BatchCollectingBuilder<V>>> part() {
		return (PartLoader<String, SmartLabel<BatchCollectingBuilder<V>>>) super.part().label(BATCH).id("_BATCH_");
	}

	public CompletableFuture<Boolean> completeBatch() {
		return completeBatch("_BATCH_");
	}

	public CompletableFuture<Boolean> completeBatch(String id) {
		return super.part().label(BATCH).id(id).value(COMPLETE).place();
	}

}
