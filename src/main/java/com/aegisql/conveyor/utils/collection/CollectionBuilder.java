package com.aegisql.conveyor.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.utils.CommonBuilder;

public class CollectionBuilder<T> extends CommonBuilder<Collection<T>> {
	
	private final Collection<T> collection;
	
	public CollectionBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.collection = new ArrayList<>();
	}

	public CollectionBuilder(long expiration ) {
		super(expiration);
		this.collection = new ArrayList<>();
	}

	public CollectionBuilder() {
		super();
		this.collection = new ArrayList<>();
	}

	public CollectionBuilder(Collection<T> collection, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.collection = collection;
	}

	public CollectionBuilder(Collection<T> collection, long expiration ) {
		super(expiration);
		this.collection = collection;
	}

	public CollectionBuilder(Collection<T> collection) {
		super();
		this.collection = collection;
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

}
