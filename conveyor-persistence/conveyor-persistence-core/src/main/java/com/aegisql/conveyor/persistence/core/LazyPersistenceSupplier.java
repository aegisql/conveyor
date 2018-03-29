/*
 * 
 */
package com.aegisql.conveyor.persistence.core;

import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class LazyPersistenceSupplier.
 */
public class LazyPersistenceSupplier implements Supplier<Persistence> {

	/** The name. */
	private final String name;

	/** The persistence. */
	private Persistence persistence = null;
	
	/**
	 * Instantiates a new lazy persistence supplier.
	 *
	 * @param name the name
	 */
	LazyPersistenceSupplier(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Persistence get() {
		if(persistence == null) {
			persistence = Persistence.byName(name);
		}
		return persistence;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.persistence = null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LazyPersistenceSupplier [name=").append(name).append(", persistence=").append(persistence)
				.append("]");
		return builder.toString();
	}
	
	
}
