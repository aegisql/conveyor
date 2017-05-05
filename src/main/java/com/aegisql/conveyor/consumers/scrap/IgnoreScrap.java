package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class IgnoreScrap.
 */
public class IgnoreScrap implements Consumer<ScrapBin<?,?>>{

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<?, ?> t) {
		//Ignore
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conveyor the conveyor
	 * @return the ignore scrap
	 */
	public static <K> IgnoreScrap of(Conveyor<K, ?, ?> conveyor) {
		return new IgnoreScrap();
	}
	
}
