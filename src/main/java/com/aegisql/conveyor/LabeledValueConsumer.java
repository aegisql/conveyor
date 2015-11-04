package com.aegisql.conveyor;

@FunctionalInterface
public interface LabeledValueConsumer<L,V,B> {
	public void accept(L label, V value, B builder);
	
	default LabeledValueConsumer<L,V,B> andThen(LabeledValueConsumer<L,V,B> after) {
		return (L l, V v, B b) -> {
			accept(l, v, b);
			after.accept(l, v, b);
		};
	}

	default LabeledValueConsumer<L,V,B> compose(LabeledValueConsumer<L,V,B> before) {
		return (L l, V v, B b) -> {
			before.accept(l, v, b);
			accept(l, v, b);
		};
	}

}
