package com.aegisql.conveyor.consumers.scrap;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LastScrapReference.
 */
public class LastScrapReference<K> implements Consumer<ScrapBin<K,?>> {

	/** The ref. */
	AtomicReference<Object> ref = new AtomicReference<>();
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,?> bin) {
		ref.set(bin.scrap);
	}
	
	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public Object getCurrent() {
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapReference [" + ref.get() + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @return the last scrap reference
	 */
	public static <K> LastScrapReference<K> of(Conveyor<K, ?, ?> conv) {
		return new LastScrapReference<>();
	}
	
}
