package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class SmartLabelBuilder.
 *
 * @param <T> the generic type
 * @param <B> the generic type
 */
public class SmartLabelBuilder<T, B> {

	/** The labels. */
	private final Map<T, SmartWrapper<T, B, ?>> labels = new HashMap<>();

	/**
	 * Instantiates a new smart label builder.
	 */
	public SmartLabelBuilder() {

	}

	/**
	 * Wrap label.
	 *
	 * @param <U> the generic type
	 * @param label the label
	 * @param consumer the consumer
	 * @return the smart label
	 */
	public <U> SmartLabel<B> wrapLabel(T label, BiConsumer<B, U> consumer) {
		SmartWrapper<T, B, U> sw = new SmartWrapper<T, B, U>(label, consumer);
		labels.put(label, sw);
		return sw;
	}

	/**
	 * Label.
	 *
	 * @param label the label
	 * @return the smart label
	 */
	public SmartLabel<B> label(T label) {
		return labels.get(label);
	}

}
