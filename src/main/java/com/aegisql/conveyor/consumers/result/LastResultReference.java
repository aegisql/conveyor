package com.aegisql.conveyor.consumers.result;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class LastResultReference <K,V> implements Consumer<ProductBin<K,V>> {

	AtomicReference<V> ref = new AtomicReference<>();
	
	@Override
	public void accept(ProductBin<K, V> bin) {
		ref.set(bin.product);
	}
	
	public V getCurrent() {
		return ref.get();
	}

	@Override
	public String toString() {
		return "ResultReference [" + ref.get() + "]";
	}

	public static <K,V> LastResultReference<K, V> of(Conveyor<K, ?, V> conv) {
		return new LastResultReference<>();
	}
	
}
