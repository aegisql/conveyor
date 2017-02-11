package com.aegisql.conveyor.utils.scalar;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.MultiKeyPartLoader;
import com.aegisql.conveyor.loaders.PartLoader;

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

	@Override
	public <X> PartLoader<K, SmartLabel<ScalarConvertingBuilder<IN, ?>>, X, OUT, Boolean> part() {
		return (PartLoader<K, SmartLabel<ScalarConvertingBuilder<IN, ?>>, X, OUT, Boolean>) super.part().label(getAddLabel(this));
	}

	@Override
	public <X> MultiKeyPartLoader<K, SmartLabel<ScalarConvertingBuilder<IN, ?>>, X, OUT, Boolean> multiKeyPart() {
		return (MultiKeyPartLoader<K, SmartLabel<ScalarConvertingBuilder<IN, ?>>, X, OUT, Boolean>) super.multiKeyPart().label(getAddLabel(this));
	}

	private SmartLabel<ScalarConvertingBuilder<IN, ?>> label = null;
	
	/**
	 * Gets the adds the label.
	 *
	 * @param <T> the generic type
	 * @return the adds the label
	 */
	private static <T> SmartLabel<ScalarConvertingBuilder<T, ?>> getAddLabel(ScalarConvertingConveyor c) {
		if( c.label == null ) {
			c.label = new SmartLabel<ScalarConvertingBuilder<T, ?>>() {
				private static final long serialVersionUID = -4838924049752143794L;
				@Override
				public BiConsumer<ScalarConvertingBuilder<T, ?>, Object> get() {
					BiConsumer<ScalarConvertingBuilder<T, ?>, T> bc = ScalarConvertingBuilder::add;
					return (BiConsumer<ScalarConvertingBuilder<T, ?>, Object>) bc;
				}
			}; 
		}
		return c.label;
	};
	
}
