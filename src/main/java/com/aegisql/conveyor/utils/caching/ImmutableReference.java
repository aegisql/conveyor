package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableReference.
 *
 * @param <T> the generic type
 */
public class ImmutableReference<T> implements Supplier<T> {

	/** The reference. */
	protected final T reference;
	
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

	public static <T> BuilderSupplier<T> newInstance(T ref) {
		return () -> new ImmutableReference<>(ref);
	}

	@Override
	public String toString() {
		return "Ref: "+reference;
	}
	
}
