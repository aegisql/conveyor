package com.aegisql.conveyor.utils.caching;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.ProductSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableReference.
 *
 * @param <T> the generic type
 */
	
public class MutableReference<T> implements ProductSupplier<T>, Consumer<T> {
	
	/** The version. */
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
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public T get() {
		return reference;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(T ref) {
		this.reference = ref;
		this.version++;
	}
	
	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public long getVersion() {
		return version;
	}
	
	/**
	 * Update.
	 *
	 * @param <T> the generic type
	 * @param builder the builder
	 * @param object the object
	 */
	public static <T> void update(MutableReference<T> builder, T object) {
		builder.accept(object);
	}
	
	/**
	 * New instance.
	 *
	 * @param <T> the generic type
	 * @param ref the ref
	 * @return the builder supplier
	 */
	public static <T> BuilderSupplier<T> newInstance(T ref) {
		return () -> new MutableReference<>(ref);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Ref ver("+version+"): "+reference;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.ProductSupplier#getSupplier()
	 */
	@Override
	public Supplier<T> getSupplier() {
		return this;
	}
	
}
