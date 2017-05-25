/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier.BuilderFutureSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class BuilderAndFutureSupplier.
 *
 * @param <T> the generic type
 */
public class BuilderAndFutureSupplier<T> implements BuilderFutureSupplier<T> {

	/** The future. */
	private final CompletableFuture<T> future;
	
	/** The builder supplier. */
	private final BuilderSupplier<? extends T> builderSupplier;
	
	/**
	 * Instantiates a new builder and future supplier.
	 *
	 * @param builderSupplier the builder supplier
	 * @param future the future
	 */
	public BuilderAndFutureSupplier(BuilderSupplier<T> builderSupplier, CompletableFuture<T> future) {
		super();
		this.builderSupplier = builderSupplier;
		this.future = future;
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public Supplier<? extends T> get() {
		return builderSupplier.get();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.FutureSupplier#getFuture()
	 */
	@Override
	public CompletableFuture<T> getFuture() {
		return future;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuilderAndFutureSupplier [builderSupplier=" + builderSupplier + ", future=" + future + "]";
	}
	
	

}
