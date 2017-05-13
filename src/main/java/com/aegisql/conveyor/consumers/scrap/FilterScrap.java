package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.ScrapBin;

public class FilterScrap <K> implements Consumer<ScrapBin<K,?>>{

	private final Predicate<ScrapBin<K,?>> filter;
	private final Consumer<ScrapBin<K,?>> action;
	
	public FilterScrap(Predicate<ScrapBin<K,?>> filter, Consumer<ScrapBin<K,?>> action) {
		this.filter = filter;
		this.action = action;
	}
	
	@Override
	public void accept(ScrapBin<K, ?> t) {
		if( filter.test(t) ) {
			action.accept(t);
		}
	}

	public static <K,V> FilterScrap <K> of(Predicate<ScrapBin<K,?>> filter, Consumer<ScrapBin<K,?>> action) {
		return new FilterScrap<>(filter, action);
	}
	
}
