package com.aegisql.conveyor.consumers.scrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LastScraps.
 *
 * @param <K> the key type
 */
public class LastScraps<K> implements ScrapConsumer<K,Object> {
	
	/** The results. */
	private final ArrayList<Object> results;
	
	/** The last. */
	private final int last;
	
	/** The p. */
	private int p = 0;
	
	/** The n. */
	private int n = -1;
	
	
	/**
	 * Instantiates a new last scraps.
	 *
	 * @param size the size
	 */
	public LastScraps(int size) {
		results = new ArrayList<>(size);
		this.last = size-1;
		for(int i =0;i<size;i++) {
			results.add(null);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,Object> bin) {
		synchronized (results) {
			results.set(p, bin.scrap);
			if(p<last) {
				p++;
			} else {
				p=0;
			}
			if(n<last) {
				n++;
			}
		}
	}
	
	/**
	 * Gets the last.
	 *
	 * @return the last
	 */
	public List<?> getLast() {
		ArrayList<Object> lastRes = new ArrayList<>(last+1);
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
		return "LastScraps [" + getLast() + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param size the size
	 * @return the last scraps
	 */
	public static <K> LastScraps<K> of(Conveyor<K, ?, ?> conv,int size) {
		return new LastScraps<>(size);
	}
	
}
