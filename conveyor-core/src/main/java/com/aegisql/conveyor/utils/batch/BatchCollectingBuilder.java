package com.aegisql.conveyor.utils.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.utils.CommonBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class BatchCollectingBuilder.
 *
 * @param <T> the generic type
 */
public class BatchCollectingBuilder<T> extends CommonBuilder<List<T>> implements TimeoutAction {
	
	/** The batch. */
	private final List<T> batch;
	
	/** The batch size. */
	private final int batchSize;
	
	/**
	 * Instantiates a new batch collecting builder.
	 *
	 * @param batchSize the batch size
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public BatchCollectingBuilder(int batchSize, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	/**
	 * Instantiates a new batch collecting builder.
	 *
	 * @param batchSize the batch size
	 * @param expiration the expiration
	 */
	public BatchCollectingBuilder(int batchSize, long expiration ) {
		super(expiration);
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	/**
	 * Instantiates a new batch collecting builder.
	 *
	 * @param batchSize the batch size
	 */
	public BatchCollectingBuilder(int batchSize ) {
		super();
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public List<T> get() {
		return batch;
	}
	
	/**
	 * Adds the.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param value the value
	 */
	public static <T> void add(BatchCollectingBuilder<T> builder, T value) {
		builder.batch.add(value);
	}

	public static <T> void complete(BatchCollectingBuilder<T> builder, BatchConveyor.BatchComplete value) {
		builder.ready = true;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.utils.CommonBuilder#test()
	 */
	@Override
	public boolean test() {
		return ready || batch.size() >= batchSize;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.TimeoutAction#onTimeout()
	 */
	@Override
	public void onTimeout() {
		ready = true;
	}
	
}
