package com.aegisql.conveyor.consumers.result;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ProductBin;

public class FilterResult <K,V> implements Consumer<ProductBin<K,V>>{

	private final Predicate<ProductBin<K,V>> filter;
	private final Consumer<ProductBin<K,V>> action;
	
	public FilterResult(Predicate<ProductBin<K,V>> filter, Consumer<ProductBin<K,V>> action) {
		this.filter = filter;
		this.action = action;
	}
	
	@Override
	public void accept(ProductBin<K, V> t) {
		if( filter.test(t) ) {
			action.accept(t);
		}
	}

	public static <K,V> FilterResult <K,V> of(Predicate<ProductBin<K,V>> filter, Consumer<ProductBin<K,V>> action) {
		return new FilterResult<>(filter, action);
	}
	
}
