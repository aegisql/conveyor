package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.serial.SerializableConsumer;
import com.aegisql.conveyor.serial.SerializablePredicate;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

// TODO: Auto-generated Javadoc
/**
 * The Interface ResultConsumer.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@FunctionalInterface
public interface ResultConsumer <K,V> extends SerializableConsumer<ProductBin<K,V>>{
	
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
	default ResultConsumer<K,V> filter(SerializablePredicate<ProductBin<K,V>> filter) {
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
	default ResultConsumer<K,V> filterKey(SerializablePredicate<K> filter) {
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
	default ResultConsumer<K,V> filterResult(SerializablePredicate<V> filter) {
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
	default ResultConsumer<K,V> filterStatus(SerializablePredicate<Status> filter) {
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
		return bin ->
			pool.submit(()->accept(bin));
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
