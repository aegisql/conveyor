package com.aegisql.conveyor.utils.collection;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

public class  CollectionBuilderLabel<T> implements SmartLabel<CollectionBuilder<T>> {

	private static final long serialVersionUID = 145340944929861103L;
	
	private BiConsumer<CollectionBuilder<T>, T> setter;
	
	CollectionBuilderLabel(BiConsumer<CollectionBuilder<T>, T> setter) {
		this.setter = setter;
	}
	
	@Override
	public BiConsumer<CollectionBuilder<T>, Object> getSetter() {
		return (BiConsumer<CollectionBuilder<T>, Object>) setter;
	}

	public static <T> CollectionBuilderLabel<T> addItemLabel() {
		BiConsumer<CollectionBuilder<T>, T> setter = CollectionBuilder::add;
		return new CollectionBuilderLabel( setter );
	}

	public static <T> CollectionBuilderLabel<T> completeCollectionLabel() {
		BiConsumer<CollectionBuilder<T>, T> setter = CollectionBuilder::complete;
		return new CollectionBuilderLabel( setter );
	}

}
