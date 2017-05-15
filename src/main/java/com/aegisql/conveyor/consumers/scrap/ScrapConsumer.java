package com.aegisql.conveyor.consumers.scrap;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;

// TODO: Auto-generated Javadoc
/**
 * The Interface ScrapConsumer.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@FunctionalInterface
public interface ScrapConsumer<K,V> extends Consumer<ScrapBin<K,V>>{

	/**
	 * And then.
	 *
	 * @param other the other
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> andThen(ScrapConsumer<K,V> other) {
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
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filter(Predicate<ScrapBin<K,V>> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	/**
	 * Filter key.
	 *
	 * @param filter the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterKey(Predicate<K> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( bin.key != null && filter.test(bin.key) ) {
	    		  accept(bin);
	    	  }
	      };	
	}

	/**
	 * Filter scrap.
	 *
	 * @param filter the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterScrap(Predicate<V> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin.scrap) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	/**
	 * Filter scrap type.
	 *
	 * @param filter the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterScrapType(Predicate<Class<?>> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test( bin.scrap.getClass() ) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}
	
	/**
	 * Filter failure type.
	 *
	 * @param filter the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterFailureType(Predicate<FailureType> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin.failureType) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	/**
	 * Filter error.
	 *
	 * @param filter the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterError(Predicate<Throwable> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( bin.error != null && filter.test(bin.error) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	/**
	 * Async.
	 *
	 * @param pool the pool
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> async(ExecutorService pool) {
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
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> async() {
		return async(ForkJoinPool.commonPool());
	}

	
}
