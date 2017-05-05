package com.aegisql.conveyor.consumers.result;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

// TODO: Auto-generated Javadoc
/**
 * The Class IgnoreResult.
 *
 * @param <K> the key type
 * @param <E> the element type
 */
public class IgnoreResult <K,E> implements Consumer<ProductBin<K,E>> {

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<K, E> t) {
		//Ignore
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the ignore result
	 */
	public static <K,E> IgnoreResult<K,E> of(Conveyor<K, ?, E> conveyor) {
		return new IgnoreResult<>();
	}

}
