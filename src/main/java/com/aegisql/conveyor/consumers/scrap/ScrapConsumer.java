package com.aegisql.conveyor.consumers.scrap;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.ScrapBin.FailureType;

@FunctionalInterface
public interface ScrapConsumer<K,V> extends Consumer<ScrapBin<K,V>>{

	default ScrapConsumer<K,V> andThen(ScrapConsumer<K,V> other) {
	      Objects.requireNonNull(other);
	      return  bin -> { 
	    	  accept(bin); 
	    	  other.accept(bin); 
	      };	
	}

	default ScrapConsumer<K,V> filter(Predicate<ScrapBin<K,V>> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	default ScrapConsumer<K,V> filterKey(Predicate<K> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( bin.key != null && filter.test(bin.key) ) {
	    		  accept(bin);
	    	  }
	      };	
	}

	default ScrapConsumer<K,V> filterScrap(Predicate<V> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin.scrap) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	default ScrapConsumer<K,V> filterScrapType(Predicate<Class<?>> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test( bin.scrap.getClass() ) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}
	
	default ScrapConsumer<K,V> filterFailureType(Predicate<FailureType> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( filter.test(bin.failureType) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	default ScrapConsumer<K,V> filterError(Predicate<Throwable> filter) {
	      Objects.requireNonNull(filter);
	      return  bin -> {
	    	  if( bin.error != null && filter.test(bin.error) ) {
	    		  accept(bin); 
	    	  }
	      };	
	}

	
}
