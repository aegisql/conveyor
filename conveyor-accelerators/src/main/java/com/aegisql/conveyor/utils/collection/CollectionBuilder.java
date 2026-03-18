package com.aegisql.conveyor.utils.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * The Class CollectionBuilder.
 *
 * @param <T> the generic type
 */
public class CollectionBuilder<T> implements Supplier<Collection<T>> {

	private final Collection<T> collection;

	public CollectionBuilder() {
		super();
		this.collection = new ArrayList<>();
	}

	@Override
	public Collection<T> get() {
		return collection;
	}

	public static <T> void add(CollectionBuilder<T> builder, T value) {
		builder.collection.add(value);
	}
}
