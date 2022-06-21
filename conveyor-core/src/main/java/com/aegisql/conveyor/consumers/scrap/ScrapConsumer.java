package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;
import com.aegisql.conveyor.serial.SerializablePredicate;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

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
	default ScrapConsumer<K,V> filter(SerializablePredicate<ScrapBin<K,V>> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	/**
	 * Filter property scrap consumer.
	 *
	 * @param property the property
	 * @param filter   the filter
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> filterProperty(String property, SerializablePredicate<Object> filter) {
		Objects.requireNonNull(property,"Property must not be null");
		Objects.requireNonNull(filter,"Predicate must be defined");
		return bin -> {
			if( bin.properties.containsKey(property) && filter.test(bin.properties.get(property)) ) {
				accept(bin);
			}
		};
	}

	/**
	 * Property equals scrap consumer.
	 *
	 * @param property the property
	 * @param obj      the obj
	 * @return the scrap consumer
	 */
	default ScrapConsumer<K,V> propertyEquals(String property, Object obj) {
		Objects.requireNonNull(property,"Property must not be null");
		return bin -> {
			if( bin.properties.containsKey(property) && Objects.equals(obj,bin.properties.get(property)) ) {
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
	default ScrapConsumer<K,V> filterKey(SerializablePredicate<K> filter) {
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
	default ScrapConsumer<K,V> filterScrap(SerializablePredicate<V> filter) {
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
	default ScrapConsumer<K,V> filterScrapType(SerializablePredicate<Class<?>> filter) {
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
	default ScrapConsumer<K,V> filterFailureType(SerializablePredicate<FailureType> filter) {
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
	default ScrapConsumer<K,V> filterError(SerializablePredicate<Throwable> filter) {
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
		return bin -> pool.submit(()-> accept(bin));
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
