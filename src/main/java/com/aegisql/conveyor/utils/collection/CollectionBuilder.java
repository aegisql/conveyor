package com.aegisql.conveyor.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.utils.CommonBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionBuilder.
 *
 * @param <T> the generic type
 */
public class CollectionBuilder<T> extends CommonBuilder<Collection<T>> {
	
	/** The collection. */
	private final Collection<T> collection;
	
	/**
	 * Instantiates a new collection builder.
	 *
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CollectionBuilder(long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.collection = new ArrayList<>();
	}

	/**
	 * Instantiates a new collection builder.
	 *
	 * @param expiration the expiration
	 */
	public CollectionBuilder(long expiration ) {
		super(expiration);
		this.collection = new ArrayList<>();
	}

	/**
	 * Instantiates a new collection builder.
	 */
	public CollectionBuilder() {
		super();
		this.collection = new ArrayList<>();
	}

	/**
	 * Instantiates a new collection builder.
	 *
	 * @param collection the collection
	 * @param ttl the ttl
	 * @param timeUnit the time unit
	 */
	public CollectionBuilder(Collection<T> collection, long ttl, TimeUnit timeUnit ) {
		super(ttl,timeUnit);
		this.collection = collection;
	}

	/**
	 * Instantiates a new collection builder.
	 *
	 * @param collection the collection
	 * @param expiration the expiration
	 */
	public CollectionBuilder(Collection<T> collection, long expiration ) {
		super(expiration);
		this.collection = collection;
	}

	/**
	 * Instantiates a new collection builder.
	 *
	 * @param collection the collection
	 */
	public CollectionBuilder(Collection<T> collection) {
		super();
		this.collection = collection;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Collection<T> get() {
		return collection;
	}
	
	/**
	 * Adds the.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param value the value
	 */
	public static <T> void add(CollectionBuilder<T> builder, T value) {
		builder.collection.add(value);
	}

	/**
	 * Complete.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param dummy the dummy
	 */
	public static <T> void complete(CollectionBuilder<T> builder, T dummy) {
		builder.setReady(true);
	}

}
