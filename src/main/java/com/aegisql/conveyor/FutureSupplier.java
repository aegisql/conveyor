package com.aegisql.conveyor;

import java.util.concurrent.CompletableFuture;

public interface FutureSupplier<T> {
	public CompletableFuture<? extends T> getFuture();
}
