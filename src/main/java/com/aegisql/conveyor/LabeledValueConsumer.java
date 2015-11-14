/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Interface LabeledValueConsumer.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <L> the generic type
 * @param <V> the value type
 * @param <B> the generic type
 */
@FunctionalInterface
public interface LabeledValueConsumer<L,V,B> {
	
	/**
	 * Accept.
	 *
	 * @param label the label
	 * @param value the value
	 * @param builder the builder
	 */
	public void accept(L label, V value, B builder);
	
	/**
	 * And then.
	 *
	 * @param after the after
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> andThen(LabeledValueConsumer<L,V,B> after) {
		return (L l, V v, B b) -> {
			accept(l, v, b);
			after.accept(l, v, b);
		};
	}

	/**
	 * Compose.
	 *
	 * @param before the before
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> compose(LabeledValueConsumer<L,V,B> before) {
		return (L l, V v, B b) -> {
			before.accept(l, v, b);
			accept(l, v, b);
		};
	}

}
