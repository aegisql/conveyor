package com.aegisql.conveyor.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionBuilder.
 *
 * @param <T> the generic type
 */
public class CollectionBuilder<T> implements Supplier<Collection<T>> {
	
	/** The collection. */
	private final Collection<T> collection;
	
	/**
	 * Instantiates a new collection builder.
	 */
	public CollectionBuilder() {
		super();
		this.collection = new ArrayList<>();
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

}
