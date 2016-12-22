/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

	default LabeledValueConsumer<L,V,B> when(L label, BiConsumer<B,V> consumer) {
		return filter(l->l.equals(label),consumer);
	}

	default LabeledValueConsumer<L,V,B> when(L label, Consumer<V> consumer) {
		return filter(l->l.equals(label),consumer);
	}

	default LabeledValueConsumer<L,V,B> when(L label, Runnable runnable) {
		return filter(l->l.equals(label),runnable);
	}

	default LabeledValueConsumer<L,V,B> filter(Predicate<L> label, BiConsumer<B,V> consumer) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( label.test(l) ) {
				consumer.accept(b, v);
			} else {
				lvc.accept(l, v, b);
			}
		};
	}

	default LabeledValueConsumer<L,V,B> filter(Predicate<L> label, Consumer<V> consumer) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( label.test(l) ) {
				consumer.accept(v);
			} else {
				lvc.accept(l, v, b);
			}
		};
	}

	default LabeledValueConsumer<L,V,B> filter(Predicate<L> label, Runnable runnable) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( label.test(l) ) {
				runnable.run();
			} else {
				lvc.accept(l, v, b);
			}
		};
	}

	default LabeledValueConsumer<L,V,B> ignore(L label) {
		return ignore(l->l.equals(label));
	}

	default LabeledValueConsumer<L,V,B> ignore(Predicate<L> label) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( ! label.test(l) ) {
				lvc.accept(l, v, b);
			}
		};
	}

}
