package com.aegisql.conveyor.utils.caching;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableReference.
 *
 * @param <T> the generic type
 */
public class ScalarReference<T> implements Supplier<T>, Consumer<T> {

	/** The reference. */
	private T reference;
	
	/**
	 * Instantiates a new immutable reference.
	 *
	 * @param ref the ref
	 */
	private ScalarReference(T ref) {
		this.reference = ref;
	}
	
	@Override
	public T get() {
		return reference;
	}
	@Override
	public void accept(T ref) {
		this.reference = ref;
	}
	
	public static <T> BuilderSupplier<T> newInstance(T ref) {
		return () -> new ScalarReference<>(ref);
	}


}
