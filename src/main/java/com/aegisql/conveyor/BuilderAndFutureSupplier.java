package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BuilderAndFutureSupplier<T> implements BuilderSupplier<T>, FutureSupplier<T> {

	private final CompletableFuture<T> future;
	private final BuilderSupplier<? extends T> builderSupplier;
	
	public BuilderAndFutureSupplier(BuilderSupplier<T> builderSupplier, CompletableFuture<T> future) {
		super();
		this.builderSupplier = builderSupplier;
		this.future = future;
	}

	@Override
	public Supplier<? extends T> get() {
		return builderSupplier.get();
	}

	@Override
	public CompletableFuture<T> getFuture() {
		return future;
	}

	@Override
	public String toString() {
		return "BuilderAndFutureSupplier [builderSupplier=" + builderSupplier + ", future=" + future + "]";
	}
	
	

}
