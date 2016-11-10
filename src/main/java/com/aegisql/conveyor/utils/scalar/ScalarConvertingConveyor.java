package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Class ScalarConvertingConveyor.
 *
 * @param <K> the key type
 * @param <IN> the generic type
 * @param <OUT> the generic type
 */
public class ScalarConvertingConveyor <K,IN,OUT> extends AssemblingConveyor<K, SmartLabel<ScalarConvertingBuilder<IN,?>>, OUT> {

	/**
	 * Instantiates a new scalar converting conveyor.
	 */
	public ScalarConvertingConveyor() {
		super();
		this.setName("ScalarConvertingConveyor");
		this.setReadinessEvaluator((state,builder) -> true ); //ready right after evaluation
	}

}
