package com.aegisql.conveyor.consumers.scrap;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class LastScrapReference implements Consumer<ScrapBin<?,?>> {

	AtomicReference<Object> ref = new AtomicReference<>();
	
	@Override
	public void accept(ScrapBin<?,?> bin) {
		ref.set(bin.scrap);
	}
	
	public Object getCurrent() {
		return ref.get();
	}

	@Override
	public String toString() {
		return "ScrapReference [" + ref.get() + "]";
	}

	public static <K> LastScrapReference of(Conveyor<K, ?, ?> conv) {
		return new LastScrapReference();
	}
	
}
