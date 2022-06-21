package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.ProductSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableReference.
 *
 * @param <T> the generic type
 */
public final class ImmutableReference<T> implements ProductSupplier<T> {

	/** The reference. */
	final T reference;
	
	/**
	 * Instantiates a new immutable reference.
	 *
	 * @param ref the ref
	 */
	private ImmutableReference(T ref) {
		this.reference = ref;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public T get() {
		return reference;
	}

	/**
	 * New instance.
	 *
	 * @param <T> the generic type
	 * @param ref the ref
	 * @return the builder supplier
	 */
	public static <T> BuilderSupplier<T> newInstance(T ref) {
		return () -> new ImmutableReference<>(ref);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Ref: "+reference;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.ProductSupplier#getSupplier()
	 */
	@Override
	public Supplier<T> getSupplier() {
		return this;
	}
	
}
