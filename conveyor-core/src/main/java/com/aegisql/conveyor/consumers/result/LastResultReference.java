package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicReference;

// TODO: Auto-generated Javadoc
/**
 * The Class LastResultReference.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LastResultReference <K,V> implements ResultConsumer<K,V> {

	@Serial
    private static final long serialVersionUID = 1L;
	/** The ref. */
	AtomicReference<V> ref = new AtomicReference<>();
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, V> bin) {
		ref.set(bin.product);
	}
	
	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public V getCurrent() {
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ResultReference [" + ref.get() + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param conv the conv
	 * @return the last result reference
	 */
	public static <K,V> LastResultReference<K, V> of(Conveyor<K, ?, V> conv) {
		return new LastResultReference<>();
	}
	
}
