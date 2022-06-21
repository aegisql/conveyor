/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.aegisql.conveyor.serial.SerializableSupplier;


// TODO: Auto-generated Javadoc
/**
 * The Interface ProductSupplier.
 *
 * @param <T> the generic type
 */
public interface ProductSupplier<T> extends SerializableSupplier<T> {
	
	/**
	 * The Interface PE.
	 *
	 * @param <T> the generic type
	 */
	//all possible permutations 
	interface PE<T>       extends ProductSupplier<T>, Expireable {}

	/**
	 * The Interface PT.
	 *
	 * @param <T> the generic type
	 */
	interface PT<T>       extends ProductSupplier<T>, Testing {}

	/**
	 * The Interface PS.
	 *
	 * @param <T> the generic type
	 * @param <K> the key type
	 * @param <L> the generic type
	 */
	interface PS<T,K,L>   extends ProductSupplier<T>, TestingState<K,L> {}

	/**
	 * The Interface PO.
	 *
	 * @param <T> the generic type
	 */
	interface PO<T>       extends ProductSupplier<T>, TimeoutAction {}

	/**
	 * The Interface PET.
	 *
	 * @param <T> the generic type
	 */
	interface PET<T>      extends PE<T>, Testing {}

	/**
	 * The Interface PES.
	 *
	 * @param <T> the generic type
	 * @param <K> the key type
	 * @param <L> the generic type
	 */
	interface PES<T,K,L>  extends PE<T>, TestingState<K,L> {}

	/**
	 * The Interface PEO.
	 *
	 * @param <T> the generic type
	 */
	interface PEO<T>      extends PE<T>, TimeoutAction {}

	/**
	 * The Interface PTO.
	 *
	 * @param <T> the generic type
	 */
	interface PTO<T>      extends PT<T>, TimeoutAction {}

	/**
	 * The Interface PSO.
	 *
	 * @param <T> the generic type
	 * @param <K> the key type
	 * @param <L> the generic type
	 */
	interface PSO<T,K,L>  extends PS<T,K,L>, TimeoutAction {}

	/**
	 * The Interface PETO.
	 *
	 * @param <T> the generic type
	 */
	interface PETO<T>     extends PET<T>, TimeoutAction {}

	/**
	 * The Interface PESO.
	 *
	 * @param <T> the generic type
	 * @param <K> the key type
	 * @param <L> the generic type
	 */
	interface PESO<T,K,L> extends PES<T,K,L>, TimeoutAction {}

	/**
	 * Gets the supplier.
	 *
	 * @return the supplier
	 */
	Supplier<T> getSupplier();
	
	/**
	 * Of.
	 *
	 * @param <T> the generic type
	 * @param instance the instance
	 * @return the product supplier
	 */
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
			@Override
			public Supplier<T> getSupplier() {
				return instance;
			}
		};
	}
	
	/**
	 * Identity.
	 *
	 * @return the product supplier
	 */
	default ProductSupplier<T> identity() {
		return this;
	}
	
/**
 * Expires.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param other the other
 * @return the product supplier
 */
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
			public Supplier<T> getSupplier() {
				return ps.getSupplier();
			}
			@Override
			public long getExpirationTime() {
				return other.getExpirationTime();
			}
		};
	}	

/**
 * On timeout.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param toAction the to action
 * @return the product supplier
 */
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
			public Supplier<T> getSupplier() {
				return ps.getSupplier();
			}
			@Override
			public void onTimeout() {
				toAction.accept(ps);
			}
		};
	}	

/**
 * Ready algorithm.
 *
 * @param <K> the key type
 * @param <L> the generic type
 * @param tester the tester
 * @return the product supplier
 */
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
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
			public Supplier<T> getSupplier() {
				return ps.getSupplier();
			}
			@Override
			public boolean test() {
				return tester.test(ps);
			}
		};
	}	

	/**
	 * Ready algorithm.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param tester the tester
	 * @return the product supplier
	 */
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
					public Supplier<T> getSupplier() {
						return ps.getSupplier();
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
					public Supplier<T> getSupplier() {
						return ps.getSupplier();
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
					public Supplier<T> getSupplier() {
						return ps.getSupplier();
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
				public Supplier<T> getSupplier() {
					return ps.getSupplier();
				}
				@Override
				public boolean test(State<K,L> t) {
					return tester.test(t, ps);
				}
			};
		}	

}
