package com.aegisql.conveyor.consumers.scrap;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class ScrapCounter <K> implements Consumer<ScrapBin<K,?>>{

	private final AtomicLong counter = new AtomicLong(0);
	
	private final Predicate<ScrapBin<K,?>> filter;
	
	public ScrapCounter() {
		this( bin -> true ); //count all
	}
	
	public ScrapCounter(Predicate<ScrapBin<K,?>> filter) {
		this.filter = filter;
	}
	
	@Override
	public void accept(ScrapBin<K,?> bin) {
		if( filter.test(bin) ) {
			counter.incrementAndGet();
		}
	}
	
	public long get() {
		return counter.get();
	}
	
	public static <K> ScrapCounter<K> of(Conveyor<K,?,?> conv) {
		return new ScrapCounter<>();
	}

	public static <K> ScrapCounter<K> of(Conveyor<K,?,?> conv, Predicate<ScrapBin<K,?>> filter) {
		return new ScrapCounter<>(filter);
	}

	@Override
	public String toString() {
		return "ScrapCounter [counter=" + counter.get() + "]";
	}
	
}
