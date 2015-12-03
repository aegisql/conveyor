package com.aegisql.conveyor.utils.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.TimeoutAction;
import com.aegisql.conveyor.utils.CommonBuilder;

public class BatchCollectingBuilder<T> extends CommonBuilder<List<T>> implements TimeoutAction {
	
	private final List<T> batch;
	private final int batchSize;
	
	public BatchCollectingBuilder(int batchSize, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	public BatchCollectingBuilder(int batchSize, long expiration ) {
		super(expiration);
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	public BatchCollectingBuilder(int batchSize ) {
		super();
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
	}

	@Override
	public List<T> get() {
		return batch;
	}
	
	public static <T> void add(BatchCollectingBuilder<T> builder, T value) {
		builder.batch.add(value);
	}

	@Override
	public boolean test() {
		return ready || batch.size() >= batchSize;
	}

	@Override
	public void onTimeout() {
		ready = true;
	}
	
}