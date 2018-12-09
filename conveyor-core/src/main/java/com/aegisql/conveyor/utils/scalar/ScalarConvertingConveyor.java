package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.loaders.PartLoader;

// TODO: Auto-generated Javadoc
/**
 * The Class ScalarConvertingConveyor.
 *
 * @param <K> the key type
 * @param <IN> the generic type
 * @param <OUT> the generic type
 */
public class ScalarConvertingConveyor <K,IN,OUT> extends AssemblingConveyor<K, String, OUT> {

	/**
	 * Instantiates a new scalar converting conveyor.
	 */
	public ScalarConvertingConveyor() {
		super();
		this.setName("ScalarConvertingConveyor");
		this.setReadinessEvaluator((state,builder) -> true ); //ready right after evaluation
		this.setDefaultCartConsumer(Conveyor.getConsumerFor(this).filter(l->true, (b,v)->{
			ScalarConvertingBuilder builder = (ScalarConvertingBuilder)b;
			ScalarConvertingBuilder.add(builder, v);
		})
				);
	}

	@Override
	public PartLoader<K, String> part() {
		return (PartLoader<K, String>) super.part().label("SCALAR");
	}


}
