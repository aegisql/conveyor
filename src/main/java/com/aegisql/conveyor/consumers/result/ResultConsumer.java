package com.aegisql.conveyor.consumers.result;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.ProductBin;

@FunctionalInterface
public interface ResultConsumer <K,V> extends Consumer<ProductBin<K,V>>{
	
	default ResultConsumer<K,V> andThen(ResultConsumer <K,V> other) {
	      Objects.requireNonNull(other);
	      return  bin -> { 
	    	  accept(bin); 
	    	  other.accept(bin); 
	      };	
	}
	
	default ResultConsumer<K,V> filter(Predicate<ProductBin<K,V>> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin) ) {
				accept(bin);
			}
		};
	}

	default ResultConsumer<K,V> filterKey(Predicate<K> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.key) ) {
				accept(bin);
			}
		};
	}

	default ResultConsumer<K,V> filterResult(Predicate<V> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.product) ) {
				accept(bin);
			}
		};
	}

	default ResultConsumer<K,V> filterStatus(Predicate<Status> filter) {
		Objects.requireNonNull(filter);
		return bin -> {
			if( filter.test(bin.status) ) {
				accept(bin);
			}
		};
	}

	default ResultConsumer<K,V> async(ExecutorService pool) {
		Objects.requireNonNull(pool);
		return bin -> {
			pool.submit(()->{
				accept(bin);
			});
		};
	}

	default ResultConsumer<K,V> async() {
		return async(ForkJoinPool.commonPool());
	}
	
}
