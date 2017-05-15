package com.aegisql.conveyor.consumers.scrap;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class ScrapCounter.
 *
 * @param <K> the key type
 */
public class ScrapCounter <K> implements ScrapConsumer<K,Object>{

	/** The counter. */
	private final AtomicLong counter = new AtomicLong(0);
	
	/** The filter. */
	private final Predicate<ScrapBin<K,?>> filter;
	
	/**
	 * Instantiates a new scrap counter.
	 */
	public ScrapCounter() {
		this( bin -> true ); //count all
	}
	
	/**
	 * Instantiates a new scrap counter.
	 *
	 * @param filter the filter
	 */
	public ScrapCounter(Predicate<ScrapBin<K,?>> filter) {
		this.filter = filter;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,Object> bin) {
		if( filter.test(bin) ) {
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
	 * @param conv the conv
	 * @return the scrap counter
	 */
	public static <K> ScrapCounter<K> of(Conveyor<K,?,?> conv) {
		return new ScrapCounter<>();
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param filter the filter
	 * @return the scrap counter
	 */
	public static <K> ScrapCounter<K> of(Conveyor<K,?,?> conv, Predicate<ScrapBin<K,?>> filter) {
		return new ScrapCounter<>(filter);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapCounter [counter=" + counter.get() + "]";
	}
	
}
