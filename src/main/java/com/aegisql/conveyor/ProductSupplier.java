package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


@FunctionalInterface
public interface ProductSupplier<T> extends Supplier<T> {
	//all possible permutations 
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
	
	static <T> ProductSupplier<T> of(Supplier<T> instance) {
		
		boolean isP = instance instanceof ProductSupplier;
		if(isP) {
			return (ProductSupplier<T>) instance;
		}
		return new ProductSupplier<T>() {
			@Override
			public T get() {
				return instance.get();
			}
		};
	}
	default ProductSupplier<T> identity() {
		return this;
	}
	
// EXPIRE //	
	default <K,L> ProductSupplier<T> expires(final Expireable other) {
		final ProductSupplier<T> ps = this;
		
		boolean isT = ps instanceof Testing;
		boolean isS = ps instanceof TestingState;
		boolean isO = ps instanceof TimeoutAction;

		if(isT && isO) {
			return new PETO<T>(){
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return other.getExpirationTime();
				}
				@Override
				public boolean test() {
					return ((Testing)ps).test();
				}
				@Override
				public void onTimeout() {
					((TimeoutAction)ps).onTimeout();
				}
			};
		}
		if(isS && isO) {
			return new PESO<T,K,L>(){
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return other.getExpirationTime();
				}
				@Override
				public void onTimeout() {
					((TimeoutAction)ps).onTimeout();
				}
				@Override
				public boolean test(State<K, L> t) {
					return ((TestingState<K, L>)ps).test(t);
				}
			};			
		}
		if(isT) {
			return new PET<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return other.getExpirationTime();
				}
				@Override
				public boolean test() {
					return ((Testing)ps).test();
				}
			};			
		}
		if(isS) {
			return new PES<T,K,L>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return other.getExpirationTime();
				}
				@Override
				public boolean test(State<K, L> t) {
					return ((TestingState<K, L>)ps).test(t);
				}
			};
			
		}
		if(isO) {
			return new PEO<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return other.getExpirationTime();
				}
				@Override
				public void onTimeout() {
					((TimeoutAction)ps).onTimeout();
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
// TIMEOUT //
	default <K,L> ProductSupplier<T> onTimeout(final Consumer<Supplier<T>> toAction) {
		final ProductSupplier<T> ps = this;
		
		boolean isE = ps instanceof Expireable;
		boolean isT = ps instanceof Testing;
		boolean isS = ps instanceof TestingState;

		if(isT && isE) {
			return new PETO<T>(){
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return ((Expireable)ps).getExpirationTime();
				}
				@Override
				public boolean test() {
					return ((Testing)ps).test();
				}
				@Override
				public void onTimeout() {
					toAction.accept(ps);					
				}
			};
		}
		if(isS && isE) {
			return new PESO<T,K,L>(){
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return ((Expireable)ps).getExpirationTime();
				}
				@Override
				public void onTimeout() {
					toAction.accept(ps);					
				}
				@Override
				public boolean test(State<K, L> t) {
					return ((TestingState<K,L>)ps).test(t);
				}
			};			
		}
		if(isT) {
			return new PTO<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public boolean test() {
					return ((Testing)ps).test();
				}
				@Override
				public void onTimeout() {
					toAction.accept(ps);					
				}
			};			
		}
		if(isS) {
			return new PSO<T,K,L>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public boolean test(State<K, L> t) {
					return ((TestingState<K, L>)ps).test(t);
				}
				@Override
				public void onTimeout() {
					toAction.accept(ps);					
				}
			};
			
		}
		if(isE) {
			return new PEO<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return ((Expireable)ps).getExpirationTime();
				}
				@Override
				public void onTimeout() {
					toAction.accept(ps);
				}
			};
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
// TESTING //
	default <K,L> ProductSupplier<T> readyAlgorithm(final Predicate<Supplier<T>> tester) {
		final ProductSupplier<T> ps = this;
		
		boolean isE = ps instanceof Expireable;
		boolean isO = ps instanceof TimeoutAction;

		if(isO && isE) {
			return new PETO<T>(){
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return ((Expireable)ps).getExpirationTime();
				}
				@Override
				public boolean test() {
					return tester.test(ps);
				}
				@Override
				public void onTimeout() {
					((TimeoutAction)ps).onTimeout();					
				}
			};
		}
		if(isO) {
			return new PTO<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public boolean test() {
					return tester.test(ps);
				}
				@Override
				public void onTimeout() {
					((TimeoutAction)ps).onTimeout();
				}
			};			
		}
		if(isE) {
			return new PET<T>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public long getExpirationTime() {
					return ((Expireable)ps).getExpirationTime();
				}
				@Override
				public boolean test() {
					return tester.test(ps);
				}
			};
		}
		return new PT<T>() {
			@Override
			public T get() {
				return ps.get();
			}
			@Override
			public boolean test() {
				return tester.test(ps);
			}
		};
	}	

	// TESTING STATE//
		default <K,L> ProductSupplier<T> readyAlgorithm(final BiPredicate<State<K,L>,ProductSupplier<? extends T>> tester) {
			final ProductSupplier<T> ps = this;
			
			boolean isE = ps instanceof Expireable;
			boolean isO = ps instanceof TimeoutAction;

			if(isO && isE) {
				return new PESO<T,K,L>(){
					@Override
					public T get() {
						return ps.get();
					}
					@Override
					public long getExpirationTime() {
						return ((Expireable)ps).getExpirationTime();
					}
					@Override
					public boolean test(State<K,L> t) {
						return tester.test(t, ps);
					}
					@Override
					public void onTimeout() {
						((TimeoutAction)ps).onTimeout();
					}
				};
			}
			if(isO) {
				return new PSO<T,K,L>() {
					@Override
					public T get() {
						return ps.get();
					}
					@Override
					public boolean test(State<K,L> t) {
						return tester.test(t, ps);
					}
					@Override
					public void onTimeout() {
						((TimeoutAction)ps).onTimeout();
					}
				};			
			}
			if(isE) {
				return new PES<T,K,L>() {
					@Override
					public T get() {
						return ps.get();
					}
					@Override
					public long getExpirationTime() {
						return ((Expireable)ps).getExpirationTime();
					}
					@Override
					public boolean test(State<K,L> t) {
						return tester.test(t, ps);
					}
				};
			}
			return new PS<T,K,L>() {
				@Override
				public T get() {
					return ps.get();
				}
				@Override
				public boolean test(State<K,L> t) {
					return tester.test(t, ps);
				}
			};
		}	

}
