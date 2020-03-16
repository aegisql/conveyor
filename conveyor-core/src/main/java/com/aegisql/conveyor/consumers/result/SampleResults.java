package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ConveyorRuntimeException;
import com.aegisql.conveyor.ProductBin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// TODO: Auto-generated Javadoc

/**
 * The Class LastResults.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class SampleResults<K,V> implements ResultConsumer<K,V> {

	private final Random rnd = new Random();

	private final double sampleRate;

	/** The results. */
	private final ArrayList<V> results;

	/** The last. */
	private final int last;

	/** The p. */
	private int p = 0;

	/** The n. */
	private int n = -1;


	/**
	 * Instantiates a new last results.
	 *
	 * @param size the size
	 */
	public SampleResults(int size, double sampleRate) {
		results = new ArrayList<>(size);
		if(sampleRate < 0 || sampleRate > 1) {
			throw new ConveyorRuntimeException("sample rate must be between 0 and 1");
		}
		this.sampleRate = sampleRate;
		this.last = size-1;
		for(int i =0;i<size;i++) {
			results.add(null);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		synchronized (results) {
			if (sampleRate == 0) {
				return;
			}
			double nextDouble = rnd.nextDouble();
			if (n < last || nextDouble < sampleRate) {
				results.set(p, bin.product);
				if (p < last) {
					p++;
				} else {
					p = 0;
				}
				if (n < last) {
					n++;
				}
			}
		}
	}
	
	/**
	 * Gets the last.
	 *
	 * @return the last
	 */
	public List<V> getLast() {
		ArrayList<V> lastRes = new ArrayList<>(last+1);
		synchronized (results) {
			int lp  = p;
			int min = Math.min(last,n);
			for(int i = 0; i <= min; i++) {
				if(lp > 0) {
					lp--;
				} else {
					lp=last;
				}
				lastRes.add(results.get(min-lp));
			}
		}
		return Collections.unmodifiableList(lastRes);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LastResults [" + getLast() + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @param size the size
	 * @return the last results
	 */
	public static <K,V> SampleResults<K, V> of(Conveyor<K, ?, V> conv, int size, double sampleRate) {
		return new SampleResults<>(size,sampleRate);
	}
	
}
