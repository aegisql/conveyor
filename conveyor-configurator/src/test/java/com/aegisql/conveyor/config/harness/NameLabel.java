package com.aegisql.conveyor.config.harness;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum NameLabel implements SmartLabel<StringSupplier>{
	
	FIRST(SmartLabel.of(StringSupplier::first).get()),
	LAST(SmartLabel.of(StringSupplier::last).get()),
	END(SmartLabel.<StringSupplier>bare().get())
	;

	
	BiConsumer<StringSupplier, Object> consumer;

	NameLabel(BiConsumer<StringSupplier, Object> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public BiConsumer<StringSupplier, Object> get() {
		return consumer;
	}

}
