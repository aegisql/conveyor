/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;

// TODO: Auto-generated Javadoc
/**
 * The Interface FutureSupplier.
 *
 * @param <T> the generic type
 */
public interface FutureSupplier<T> {
	
	/**
	 * Gets the future.
	 *
	 * @return the future
	 */
	CompletableFuture<? extends T> getFuture();
}
