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
public class MutableReference<T> implements Supplier<T>, Consumer<T> {
	
	protected long version = 1;

	/** The reference. */
	protected T reference;
	
	/**
	 * Instantiates a new immutable reference.
	 *
	 * @param ref the ref
	 */
	protected MutableReference(T ref) {
		this.reference = ref;
	}
	
	@Override
	public T get() {
		return reference;
	}
	@Override
	public void accept(T ref) {
		this.reference = ref;
		this.version++;
	}
	
	public long getVersion() {
		return version;
	}
	
	public static <T> void update(MutableReference<T> builder, T object) {
		builder.accept(object);
	}
	
	public static <T> BuilderSupplier<T> newInstance(T ref) {
		return () -> new MutableReference<>(ref);
	}

	@Override
	public String toString() {
		return "Ref ver("+version+"): "+reference;
	}
	
}
