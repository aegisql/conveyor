package com.aegisql.conveyor;

import java.util.function.Consumer;
import java.util.function.Supplier;


@FunctionalInterface
public interface ProductSupplier<T> extends Supplier<T> {
	
	interface PE<T>       extends ProductSupplier<T>, Expireable {};
	interface PT<T>       extends ProductSupplier<T>, Testing {};
	interface PS<T,K,L>   extends ProductSupplier<T>, TestingState<K,L> {};
	interface PO<T>       extends ProductSupplier<T>, TimeoutAction {};

	interface PET<T>      extends PE<T>, Testing {};
	interface PES<T,K,L>  extends PE<T>, TestingState<K,L> {};
	interface PEO<T>      extends PE<T>, TimeoutAction {};

	interface PTO<T>      extends PT<T>, TimeoutAction {};
	interface PSO<T,K,L>  extends PS<T,K,L>, TimeoutAction {};

	interface PETO<T>     extends PET<T>, TimeoutAction {};
	interface PESO<T,K,L> extends PES<T,K,L>, TimeoutAction {};
	
	static <T> ProductSupplier<T> of(ProductSupplier<T> instance) {
		return instance;
	}
	default ProductSupplier<T> identity() {
		return this;
	}
	default ProductSupplier<T> expire(final Expireable other) {
		final ProductSupplier<T> ps = this;
		
		boolean isE = ps instanceof Expireable;
		boolean isT = ps instanceof Testing;
		boolean isS = ps instanceof TestingState;
		boolean isO = ps instanceof TimeoutAction;

		if(isE) {
			throw new RuntimeException("Already instance of expireable");
		}
		
		
		if(isT && isO) {
			
		}
		if(isS && isO) {
			
		}
		if(isT) {
			return new PET<T>() {
				@Override
				public T get() {
					return null;
				}
				@Override
				public long getExpirationTime() {
					return 0;
				}
				@Override
				public boolean test() {
					return false;
				}
			};
			
		}
		if(isS) {
			
		}
		if(isO) {
			return new PEO<T>() {
				@Override
				public T get() {
					return null;
				}
				@Override
				public long getExpirationTime() {
					return 0;
				}
				@Override
				public void onTimeout() {
				}
			};
		}
		return new PE<T>() {
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
		
		return new PO<T>() {
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
