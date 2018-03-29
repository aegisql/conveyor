package com.aegisql.conveyor.persistence.core;

import java.util.function.Supplier;

public class LazyPersistenceSupplier implements Supplier<Persistence> {

	private final String name;

	private Persistence persistence = null;
	
	LazyPersistenceSupplier(String name) {
		this.name = name;
	}

	@Override
	public Persistence get() {
		if(persistence == null) {
			persistence = Persistence.byName(name);
		}
		return persistence;
	}

	public void reset() {
		this.persistence = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LazyPersistenceSupplier [name=").append(name).append(", persistence=").append(persistence)
				.append("]");
		return builder.toString();
	}
	
	
}
