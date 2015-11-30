package com.aegisql.conveyor.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;

public class CollectionBuilder<T> implements Supplier<Collection<T>>,Testing, Delayed {
	
	private final Collection<T> collection;
	
	private final long builderCreated = System.currentTimeMillis();
	private final long builderExpiration;

	private boolean ready = false;
	
	public CollectionBuilder(long ttl, TimeUnit timeUnit ) {
		this.collection = new ArrayList<>();
		this.builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	public CollectionBuilder(long expiration ) {
		this.collection = new ArrayList<>();
		this.builderExpiration = expiration;
	}

	public CollectionBuilder() {
		this.collection = new ArrayList<>();
		this.builderExpiration = 0;
	}

	public CollectionBuilder(Collection<T> collection, long ttl, TimeUnit timeUnit ) {
		this.collection = collection;
		this.builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
	}

	public CollectionBuilder(Collection<T> collection, long expiration ) {
		this.collection = collection;
		this.builderExpiration = expiration;
	}

	public CollectionBuilder(Collection<T> collection) {
		this.collection = collection;
		this.builderExpiration = 0;
	}

	@Override
	public Collection<T> get() {
		return collection;
	}
	
	public static <T> void add(CollectionBuilder<T> builder, T value) {
		builder.collection.add(value);
	}

	public static <T> void complete(CollectionBuilder<T> builder, T dummy) {
		builder.setReady(true);
	}

	@Override
	public boolean test() {
		return ready;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed o) {
		return (int) (builderCreated - ((CollectionBuilder<T>)o).builderCreated);
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

	public void setReady(boolean ready) {
		this.ready = ready;
	}

}
