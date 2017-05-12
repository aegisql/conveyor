package com.aegisql.conveyor.consumers.result;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class ResultCounter <K,V> implements Consumer<ProductBin<K,V>>{

	private final AtomicLong counter = new AtomicLong(0);
	
	private final Predicate<V> filter;
	
	public ResultCounter() {
		this( bin -> true ); //count all
	}
	
	public ResultCounter(Predicate<V> filter) {
		this.filter = filter;
	}
	
	@Override
	public void accept(ProductBin<K, V> bin) {
		if( filter.test(bin.product) ) {
			counter.incrementAndGet();
		}
	}
	
	public long get() {
		return counter.get();
	}
	
	public static <K,V> ResultCounter<K,V> of(Conveyor<K,?,V> conv) {
		return new ResultCounter<>();
	}

	public static <K,V> ResultCounter<K,V> of(Conveyor<K,?,V> conv, Predicate<V> filter) {
		return new ResultCounter<>(filter);
	}

	@Override
	public String toString() {
		return "ResultCounter [counter=" + counter.get() + "]";
	}
	
}
