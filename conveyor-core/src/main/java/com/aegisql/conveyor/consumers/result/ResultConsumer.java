package com.aegisql.conveyor.consumers.result;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;

// TODO: Auto-generated Javadoc
/**
 * The Interface ResultConsumer.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@FunctionalInterface
public interface ResultConsumer <K,V> extends Consumer<ProductBin<K,V>>{
	
	/**
	 * And then.
	 *
	 * @param other the other
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> andThen(ResultConsumer <K,V> other) {
	      Objects.requireNonNull(other);
	      return  bin -> { 
	    	  accept(bin); 
	    	  other.accept(bin); 
	      };	
	}
	
	/**
	 * Filter.
	 *
	 * @param filter the filter
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> filter(Predicate<ProductBin<K,V>> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin) ) {
				accept(bin);
			}
		};
	}

	/**
	 * Filter key.
	 *
	 * @param filter the filter
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> filterKey(Predicate<K> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.key) ) {
				accept(bin);
			}
		};
	}

	/**
	 * Filter result.
	 *
	 * @param filter the filter
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> filterResult(Predicate<V> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.product) ) {
				accept(bin);
			}
		};
	}

	/**
	 * Filter status.
	 *
	 * @param filter the filter
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> filterStatus(Predicate<Status> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.status) ) {
				accept(bin);
			}
		};
	}

	/**
	 * Async.
	 *
	 * @param pool the pool
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> async(ExecutorService pool) {
		Objects.requireNonNull(pool);
		return bin -> {
			pool.submit(()->{
				accept(bin);
			});
		};
	}

	/**
	 * Async.
	 *
	 * @return the result consumer
	 */
	default ResultConsumer<K,V> async() {
		return async(ForkJoinPool.commonPool());
	}

}
