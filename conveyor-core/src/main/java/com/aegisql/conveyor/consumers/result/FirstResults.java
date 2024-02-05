package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Class FirstResults.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class FirstResults <K,V> implements ResultConsumer<K,V> {
	
	/** The results. */
	private final ArrayList<V> results;
	
	/** The max. */
	private final int max;
	
	/** The p. */
	private int p = 0;	
	
	/**
	 * Instantiates a new first results.
	 *
	 * @param size the size
	 */
	public FirstResults(int size) {
		results = new ArrayList<>(size);
		this.max = size;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		synchronized (results) {
			if(p < max) {
				results.add(bin.product);
				p++;
			}
		}
	}
	
	/**
	 * Gets the first.
	 *
	 * @return the first
	 */
	public List<V> getFirst() {
		return Collections.unmodifiableList(results);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FirstResults [" + results + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param size the size
	 * @return the first results
	 */
	public static <K,V> FirstResults<K, V> of(Conveyor<K, ?, V> conv,int size) {
		return new FirstResults<>(size);
	}
	
}
