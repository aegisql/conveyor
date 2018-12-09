package com.aegisql.conveyor.loaders;

import java.util.function.Supplier;

public class LazyPartLoaderSupplier <K,L> implements Supplier<PartLoader<K,L>> {

	private final String name;
	
	private PartLoader<K,L> loader;
	
	public LazyPartLoaderSupplier(String name) {
		this.name = name;
	}

	@Override
	public PartLoader<K,L> get() {
		if(loader == null) {
			loader = PartLoader.byConveyorName(name);
		}
		return loader;
	}

}
