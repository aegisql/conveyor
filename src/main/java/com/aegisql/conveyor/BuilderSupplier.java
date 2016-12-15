package com.aegisql.conveyor;

import java.util.function.Supplier;

/**
 * The Interface BuilderSupplier.
 *
 * @param <T> the generic type
 */
public interface BuilderSupplier <T> extends Supplier<Supplier<? extends T>> {
	
	interface ExpireableBuilderSupplier<T> extends BuilderSupplier<T>, Expireable {};
	interface TestingBuilderSupplier<T> extends BuilderSupplier<T>, Testing {};
	interface TestingStateBuilderSupplier<T,K,L> extends BuilderSupplier<T>, TestingState<K,L> {};
	interface TimingOutBuilderSupplier<T> extends BuilderSupplier<T>, TimeoutAction {};

	default BuilderSupplier <T> expire(long expTime) {
		
		BuilderSupplier <T> bse = new ExpireableBuilderSupplier<T>() {

			@Override
			public Supplier<? extends T> get() {
				return this.get();
			}

			@Override
			public long getExpirationTime() {
				return 0;
			}
		};
		return bse;
	}
	
	
}
