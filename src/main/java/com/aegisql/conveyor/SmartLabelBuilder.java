package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SmartLabelBuilder<T, B> {

	private final Map<T, SmartWrapper<T, B, ?>> labels = new HashMap<>();

	public SmartLabelBuilder() {

	}

	public <U> SmartLabel<B> wrapLabel(T label, BiConsumer<B, U> consumer) {
		SmartWrapper<T, B, U> sw = new SmartWrapper<T, B, U>(label, consumer);
		labels.put(label, sw);
		return sw;
	}

	public SmartLabel<B> label(T label) {
		return labels.get(label);
	}

}
