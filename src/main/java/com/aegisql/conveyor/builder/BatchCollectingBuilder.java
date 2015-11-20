package com.aegisql.conveyor.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;

public class BatchCollectingBuilder<T> implements Supplier<List<T>>,Testing, Delayed {
	
	private final List<T> batch;
	private final int batchSize;
	
	private final long builderCreated = System.currentTimeMillis();
	private final long builderExpiration;

	
	public BatchCollectingBuilder(int batchSize, long ttl, TimeUnit timeUnit ) {
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
		this.builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	public BatchCollectingBuilder(int batchSize, long expiration ) {
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
		this.builderExpiration = expiration;
	}

	public BatchCollectingBuilder(int batchSize ) {
		this.batch = new ArrayList<>( batchSize );
		this.batchSize = batchSize;
		this.builderExpiration = 0;
	}

	@Override
	public List<T> get() {
		return batch;
	}
	
	public static <T> void add(BatchCollectingBuilder<T> builder, T value) {
		builder.batch.add(value);
	}

	public static <T> List<T> getOnTimeout(BatchCollectingBuilder<T> builder) {
		return builder.batch;
	}
	
	@Override
	public boolean test() {
		return batch.size() >= batchSize;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed o) {
		return (int) (builderCreated - ((BatchCollectingBuilder<T>)o).builderCreated);
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( builderExpiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = builderExpiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}
	
}
