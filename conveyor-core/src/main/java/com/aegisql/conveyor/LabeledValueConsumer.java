/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/**
	 * When.
	 *
	 * @param <T> the generic type
	 * @param label the label
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
	default <T> LabeledValueConsumer<L,V,B> when(L label, BiConsumer<B,T> consumer) {
		return filter(l->l.equals(label),consumer);
	}

	/**
	 * When.
	 *
	 * @param label the label
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> when(L label, Consumer<V> consumer) {
		return filter(l->l.equals(label),consumer);
	}

	/**
	 * When.
	 *
	 * @param label the label
	 * @param runnable the runnable
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> when(L label, Runnable runnable) {
		return filter(l->l.equals(label),runnable);
	}

	/**
	 * Filter.
	 *
	 * @param <T> the generic type
	 * @param label the label
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
	default <T> LabeledValueConsumer<L,V,B> filter(Predicate<L> label, BiConsumer<B,T> consumer) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( label.test(l) ) {
				consumer.accept(b, (T)v);
			} else {
				lvc.accept(l, v, b);
			}
		};
	}

	/**
	 * Filter.
	 *
	 * @param label the label
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
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

	/**
	 * Filter.
	 *
	 * @param label the label
	 * @param runnable the runnable
	 * @return the labeled value consumer
	 */
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

	/**
	 * Match.
	 *
	 * @param <T> the generic type
	 * @param pattern the pattern
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
	default <T> LabeledValueConsumer<L,V,B> match(String pattern, BiConsumer<B,T> consumer) {
		final Pattern p = Pattern.compile(pattern);
		return filter(l->{
			Matcher m = p.matcher(l.toString());
			return m.matches();
		},consumer);
	}

	/**
	 * Match.
	 *
	 * @param pattern the pattern
	 * @param consumer the consumer
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> match(String pattern, Consumer<V> consumer) {
		final Pattern p = Pattern.compile(pattern);
		return filter(l->{
			Matcher m = p.matcher(l.toString());
			return m.matches();
		},consumer);
	}

	/**
	 * Match.
	 *
	 * @param pattern the pattern
	 * @param runnable the runnable
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> match(String pattern, Runnable runnable) {
		final Pattern p = Pattern.compile(pattern);
		return filter(l->{
			Matcher m = p.matcher(l.toString());
			return m.matches();
		},runnable);
	}

	
	/**
	 * Ignore.
	 *
	 * @param label the label
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> ignore(L label) {
		return ignore(l->l.equals(label));
	}

	/**
	 * Ignore.
	 *
	 * @param label the label
	 * @return the labeled value consumer
	 */
	default LabeledValueConsumer<L,V,B> ignore(Predicate<L> label) {
		LabeledValueConsumer<L,V,B> lvc = this;
		return (l,v,b)->{
			if( ! label.test(l) ) {
				lvc.accept(l, v, b);
			}
		};
	}
	
}
