package com.aegisql.conveyor.persistence.core.harness;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public enum TrioPartExpireable implements SmartLabel<TrioBuilderExpireable>{
	TEXT1(SmartLabel.of(TrioBuilderExpireable::setText1)),
	TEXT2(SmartLabel.of(TrioBuilderExpireable::setText2)),
	NUMBER(SmartLabel.of(TrioBuilderExpireable::setNumber))
	;
	private final SmartLabel<TrioBuilderExpireable> inner;
	<T> TrioPartExpireable(SmartLabel<TrioBuilderExpireable> inner) {
		this.inner = inner;
	}
	@Override
	public BiConsumer<TrioBuilderExpireable, Object> get() {
		return inner.get();
	}
}
