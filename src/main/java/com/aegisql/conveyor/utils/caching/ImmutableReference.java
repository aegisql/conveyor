package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableReference.
 *
 * @param <T> the generic type
 */
public class ImmutableReference<T> implements BuilderSupplier<T> {

	/** The reference. */
	private final T reference;
	
	/**
	 * Instantiates a new immutable reference.
	 *
	 * @param ref the ref
	 */
	public ImmutableReference(T ref) {
		this.reference = ref;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Supplier<? extends T> get() {
		return () -> reference;
	}

}
