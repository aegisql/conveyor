/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Interface BuilderSupplier.
 *
 * @param <T> the generic type
 */
public interface BuilderSupplier<T> extends Supplier<Supplier<? extends T>>, Serializable {

	/**
	 * The Interface BuilderFutureSupplier.
	 *
	 * @param <T> the generic type
	 */
	interface BuilderFutureSupplier<T> extends BuilderSupplier<T>, FutureSupplier<T>, Serializable {}

	/**
	 * Of.
	 *
	 * @param <T> the generic type
	 * @param instance the instance
	 * @return the builder supplier
	 */
	static <T> BuilderSupplier<T> of(BuilderSupplier<T> instance) {
		return instance;
	}

	/**
	 * Of.
	 *
	 * @param <T> the generic type
	 * @param instance the instance
	 * @return the builder supplier
	 */
	static <T> BuilderSupplier<T> of(Supplier<? extends T> instance) {
		return ()->instance;
	}
	
	/**
	 * Identity.
	 *
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> identity() {
		return this;
	}
	
	/**
	 * Expire.
	 *
	 * @param expTime the exp time
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> expire(long expTime) {
		return expire( () -> expTime );
	}
	
	/**
	 * Expire.
	 *
	 * @param ttl the ttl
	 * @param unit the unit
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> expire(long ttl, TimeUnit unit) {
		return expire(System.currentTimeMillis() + unit.toMillis(ttl));
	}	
	
	/**
	 * Expire.
	 *
	 * @param duration the duration
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> expire(Duration duration) {
		return expire(System.currentTimeMillis() + duration.toMillis());
	}	
	
	/**
	 * Expire.
	 *
	 * @param instant the instant
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> expire(Instant instant) {
		return expire(instant::toEpochMilli);
	}	
	
	/**
	 * Expire.
	 *
	 * @param other the other
	 * @return the builder supplier
	 */
	default BuilderSupplier <T> expire(Expireable other) {
		BuilderSupplier <T> bs = this;
		return () -> ProductSupplier.of(bs.get()).expires(other);
	}	

	/**
	 * Ready algorithm.
	 *
	 * @param tester the tester
	 * @return the builder supplier
	 */
	default BuilderSupplier<T> readyAlgorithm(Predicate<Supplier<? extends T>> tester) {
		BuilderSupplier <T> bs = this;
		return () -> ProductSupplier.of(bs.get()).readyAlgorithm((Predicate) tester);
	}

	/**
	 * Ready algorithm.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param tester the tester
	 * @return the builder supplier
	 */
	default <K,L> BuilderSupplier<T> readyAlgorithm(BiPredicate<State<K,L>, Supplier<? extends T>> tester) {
		BuilderSupplier <T> bs = this;
		return () -> ProductSupplier.of(bs.get()).readyAlgorithm((BiPredicate) tester);
	}

	/**
	 * On timeout.
	 *
	 * @param consumer the consumer
	 * @return the builder supplier
	 */
	default BuilderSupplier<T> onTimeout(Consumer<Supplier<? extends T>> consumer) {
		BuilderSupplier <T> bs = this;
		return () -> ProductSupplier.of(bs.get()).onTimeout((Consumer) consumer);
	}
	
	/**
	 * With future.
	 *
	 * @param future the future
	 * @return the builder supplier
	 */
	default BuilderSupplier<T> withFuture(CompletableFuture<T> future) {
		BuilderSupplier <T> bs = this;
		return new BuilderFutureSupplier<>() {
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
