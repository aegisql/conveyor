package com.aegisql.conveyor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SmartLabelBuilder<T,B> {

	private final Map<T,SmartLabel<B>> labels = new HashMap<>();
	
	public SmartLabelBuilder() {
		
	}
	
	 public <U> void addLabel(T label, BiConsumer<B, U> consumer) {
		labels.put(label, new SmartLabel<B>() {
			private static final long serialVersionUID = 1L;
			@Override
			public BiConsumer<B, Object> get() {
				return (BiConsumer<B, Object>) consumer;
			}});
	}

	public SmartLabel<B> label(T key) {
		return labels.get(key);
	}
	
}
