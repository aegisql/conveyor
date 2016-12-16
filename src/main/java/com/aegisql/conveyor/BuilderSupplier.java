package com.aegisql.conveyor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The Interface BuilderSupplier.
 *
 * @param <T> the generic type
 */
public interface BuilderSupplier<T> extends Supplier<Supplier<? extends T>> {
	
	interface ExpireableBuilderSupplier<T> extends Supplier<T>, Expireable {};
	interface TestingBuilderSupplier<T> extends Supplier<T>, Testing {};
	interface TestingStateBuilderSupplier<T,K,L> extends Supplier<T>, TestingState<K,L> {};
	interface TimingOutBuilderSupplier<T> extends Supplier<T>, TimeoutAction {};
	interface BuilderFutureSupplier<T> extends BuilderSupplier<T>, FutureSupplier<T> {};

	static <T> BuilderSupplier<T> of(BuilderSupplier<T> instance) {
		return instance;
	}
	default BuilderSupplier <T> identity() {
		return this;
	}
	default BuilderSupplier <T> expire(long expTime) {
		return expire( () -> expTime );
	}
	default BuilderSupplier <T> expire(long ttl, TimeUnit unit) {
		return expire(System.currentTimeMillis() + unit.toMillis(ttl));
	}	
	default BuilderSupplier <T> expire(Duration duration) {
		return expire(System.currentTimeMillis() + duration.toMillis());
	}	
	default BuilderSupplier <T> expire(Instant instant) {
		return expire(instant::toEpochMilli);
	}	
	default BuilderSupplier <T> expire(Expireable other) {
		BuilderSupplier <T> bs = this;
		return new BuilderSupplier<T>() {
			@Override
			public Supplier<? extends T> get() {
				return new ExpireableBuilderSupplier<T>() {
					@Override
					public T get() {
						return bs.get().get();
					}

					@Override
					public long getExpirationTime() {
						return other.getExpirationTime();
					}
				};
			}
		};
	}	

	default BuilderSupplier<T> test(Predicate<Supplier<? extends T>> tester) {
		BuilderSupplier <T> bs = this;
		return new BuilderSupplier<T>() {
			@Override
			public Supplier<? extends T> get() {
				return new TestingBuilderSupplier<T>() {
					Supplier<? extends T> builder = null;
					@Override
					public T get() {
						if(builder == null) {
							builder = bs.get();
						}
						return builder.get();
					}

					@Override
					public boolean test() {
						return tester.test( builder );
					}
				};
			}
		};
	}

	default <K,L> BuilderSupplier<T> testState(BiPredicate<State<K,L>, Supplier<? extends T>> tester) {
		BuilderSupplier <T> bs = this;
		return new BuilderSupplier<T>() {
			@Override
			public Supplier<? extends T> get() {
				return new TestingStateBuilderSupplier<T,K,L>() {
					Supplier<? extends T> builder = null;
					@Override
					public T get() {
						if(builder == null) {
							builder = bs.get();
						}
						return builder.get();
					}
					@Override
					public boolean test(State<K,L> state) {
						return false;
					}

				};
			}
		};
	}

	default BuilderSupplier<T> timeout(Consumer<Supplier<? extends T>> consumer) {
		BuilderSupplier <T> bs = this;
		return new BuilderSupplier<T>() {
			@Override
			public Supplier<? extends T> get() {
				return new TimingOutBuilderSupplier<T>() {
					Supplier<? extends T> builder = null;
					@Override
					public T get() {
						if(builder == null) {
							builder = bs.get();
						}
						return builder.get();
					}
					@Override
					public void onTimeout() {
						consumer.accept(builder);
					}
				};
			}
		};
	}
	
	default BuilderSupplier<T> withFuture(CompletableFuture<T> future) {
		BuilderSupplier <T> bs = this;
		return new BuilderFutureSupplier<T>() {
			@Override
			public Supplier<? extends T> get() {
				return bs.get();
			}
			@Override
			public CompletableFuture<? extends T> getFuture() {
				return future;
			}
		};
	}
	
}
