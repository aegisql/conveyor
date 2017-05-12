package com.aegisql.conveyor.consumers.scrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class FirstScraps.
 *
 * @param <K> the key type
 */
public class FirstScraps<K> implements Consumer<ScrapBin<K,?>> {
	
	/** The scraps. */
	private final ArrayList<Object> scraps;
	
	/** The max. */
	private final int max;
	
	/** The p. */
	private int p = 0;	
	
	/**
	 * Instantiates a new first scraps.
	 *
	 * @param size the size
	 */
	public FirstScraps(int size) {
		scraps = new ArrayList<>(size);
		this.max = size;
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,?> bin) {
		synchronized (scraps) {
			if(p < max) {
				scraps.add(bin.scrap);
				p++;
			}
		}
	}
	
	/**
	 * Gets the first.
	 *
	 * @return the first
	 */
	public List<?> getFirst() {
		return Collections.unmodifiableList(scraps);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FirstScraps [" + scraps + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @param size the size
	 * @return the first scraps
	 */
	public static <K> FirstScraps<K> of(Conveyor<K, ?, ?> conv,int size) {
		return new FirstScraps<>(size);
	}
	
}
