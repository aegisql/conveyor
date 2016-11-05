package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BuilderAndFutureSupplier<T> implements BuilderSupplier<T>, FutureSupplier<T> {

	private final CompletableFuture<? extends T> future;
	private final Supplier<? extends T> builderSupplier;
	
	public BuilderAndFutureSupplier(Supplier<? extends T> builderSupplier, CompletableFuture<? extends T> future) {
		super();
		this.builderSupplier = builderSupplier;
		this.future = future;
	}

	@Override
	public Supplier<? extends T> get() {
		return builderSupplier;
	}

	@Override
	public CompletableFuture<? extends T> getFuture() {
		return future;
	}

}
