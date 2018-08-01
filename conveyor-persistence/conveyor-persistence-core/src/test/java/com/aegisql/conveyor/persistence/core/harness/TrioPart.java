package com.aegisql.conveyor.persistence.core.harness;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilder;

public enum TrioPart implements SmartLabel<TrioBuilder> {
	TEXT1(SmartLabel.of(TrioBuilder::setText1)),
	TEXT2(SmartLabel.of(TrioBuilder::setText2)),
	NUMBER(SmartLabel.of(TrioBuilder::setNumber))
	;
	private final SmartLabel<TrioBuilder> inner;
	<T> TrioPart(SmartLabel<TrioBuilder> inner) {
		this.inner = inner;
	}
	@Override
	public BiConsumer<TrioBuilder, Object> get() {
		return inner.get();
	}
}
