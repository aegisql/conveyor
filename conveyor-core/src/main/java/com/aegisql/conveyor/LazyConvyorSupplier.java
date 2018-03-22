package com.aegisql.conveyor;

import java.util.function.Supplier;

/**
 * The Class LazyConvyorSupplier.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param <OUT> the generic type
 */
public class LazyConvyorSupplier <K,L,OUT> implements Supplier<Conveyor<K,L,OUT>> {

	/** The name. */
	private final String name;
	
	/** The conveyor. */
	private Conveyor<K,L,OUT> conveyor;
	
	/**
	 * Instantiates a new lazy convyor supplier.
	 *
	 * @param name the name
	 */
	LazyConvyorSupplier(String name) {
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Conveyor<K, L, OUT> get() {
		if(conveyor == null) {
			conveyor = Conveyor.byName(name);
		}
		return conveyor;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		conveyor = null;
	}
	
}
