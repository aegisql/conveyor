package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class IgnoreScrap.
 *
 * @param <K> the key type
 */
public class IgnoreScrap<K> implements ScrapConsumer<K,Object>{

	public final static IgnoreScrap INSTANCE = new IgnoreScrap();

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K, Object> t) {
		//Ignore
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the ignore scrap
	 */
	public static <K> IgnoreScrap<K> of(Conveyor<K, ?, ?> conveyor) {
		return new IgnoreScrap<>();
	}
	
}
