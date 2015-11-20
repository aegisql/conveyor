package com.aegisql.conveyor.builder;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public final class BatchLabel<T> implements SmartLabel<BatchCollectingBuilder<T>> {

	private static final long serialVersionUID = 1L;

	BiConsumer<BatchCollectingBuilder<T>, T> setter;

	public BatchLabel() {
		this.setter = BatchCollectingBuilder::add;
	}
	
	@Override
	public BiConsumer<BatchCollectingBuilder<T>, Object> getSetter() {
		return (BiConsumer<BatchCollectingBuilder<T>, Object>) setter;
	}
	

}
