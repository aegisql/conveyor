package com.aegisql.conveyor;

import java.util.function.Consumer;
import java.util.function.Supplier;


@FunctionalInterface
public interface ProductSupplier<T> extends Supplier<T> {
	
	interface EPS<T>        extends ProductSupplier<T>, Expireable {};
	interface TPS<T>        extends ProductSupplier<T>, Testing {};
	interface TSPS<T,K,L>   extends ProductSupplier<T>, TestingState<K,L> {};
	interface TOPS<T>       extends ProductSupplier<T>, TimeoutAction {};

	interface ETPS<T>       extends EPS<T>, Testing {};
	interface ETSPS<T,K,L>  extends EPS<T>, TestingState<K,L> {};
	interface ETOPS<T>      extends EPS<T>, TimeoutAction {};

	interface TTOPS<T>      extends TPS<T>, TimeoutAction {};
	interface TSTOPS<T,K,L> extends TSPS<T,K,L>, TimeoutAction {};

	
	static <T> ProductSupplier<T> of(ProductSupplier<T> instance) {
		return instance;
	}
	default ProductSupplier<T> identity() {
		return this;
	}
	default ProductSupplier<T> expire(final Expireable other) {
		final ProductSupplier<T> ps = this;
		return new EPS<T>() {
			@Override
			public T get() {
				return ps.get();
			}
			@Override
			public long getExpirationTime() {
				return other.getExpirationTime();
			}
		};
	}	

	default ProductSupplier<T> timeout(final Consumer<ProductSupplier<T>> toAction) {
		final ProductSupplier<T> ps = this;
		
		if(this instanceof Expireable) {
			
		}
		
		return new TOPS<T>() {
			@Override
			public T get() {
				return ps.get();
			}
			@Override
			public void onTimeout() {
				toAction.accept(ps);
			}
		};
	}	

}
