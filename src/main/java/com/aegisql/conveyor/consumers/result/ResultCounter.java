package com.aegisql.conveyor.consumers.result;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultCounter.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ResultCounter <K,V> implements Consumer<ProductBin<K,V>>{

	/** The counter. */
	private final AtomicLong counter = new AtomicLong(0);
	
	/** The filter. */
	private final Predicate<V> filter;
	
	/**
	 * Instantiates a new result counter.
	 */
	public ResultCounter() {
		this( bin -> true ); //count all
	}
	
	/**
	 * Instantiates a new result counter.
	 *
	 * @param filter the filter
	 */
	public ResultCounter(Predicate<V> filter) {
		this.filter = filter;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		if( filter.test(bin.product) ) {
			counter.incrementAndGet();
		}
	}
	
	/**
	 * Gets the.
	 *
	 * @return the long
	 */
	public long get() {
		return counter.get();
	}
	
	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @return the result counter
	 */
	public static <K,V> ResultCounter<K,V> of(Conveyor<K,?,V> conv) {
		return new ResultCounter<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param filter the filter
	 * @return the result counter
	 */
	public static <K,V> ResultCounter<K,V> of(Conveyor<K,?,V> conv, Predicate<V> filter) {
		return new ResultCounter<>(filter);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultCounter [counter=" + counter.get() + "]";
	}
	
}
