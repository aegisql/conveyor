package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicLong;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultCounter.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class ResultCounter <K,V> implements ResultConsumer<K,V> {

	@Serial
    private static final long serialVersionUID = 1L;
	/** The counter. */
	private final AtomicLong counter = new AtomicLong(0);
	
	/**
	 * Instantiates a new result counter.
	 */
	public ResultCounter() {
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		counter.incrementAndGet();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultCounter [counter=" + counter.get() + "]";
	}
	
}
