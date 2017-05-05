package com.aegisql.conveyor.consumers.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class FirstResults <K,V> implements Consumer<ProductBin<K,V>> {
	
	private final ArrayList<V> results;
	private final int max;
	private int p = 0;	
	
	public FirstResults(int size) {
		results = new ArrayList<>(size);
		this.max = size;
	}
	
	@Override
	public void accept(ProductBin<K, V> bin) {
		synchronized (results) {
			if(p < max) {
				results.add(bin.product);
				p++;
			}
		}
	}
	
	public List<V> getFirst() {
		return Collections.unmodifiableList(results);
	}

	@Override
	public String toString() {
		return "FirstResults [" + results + "]";
	}

	public static <K,V> FirstResults<K, V> of(Conveyor<K, ?, V> conv,int size) {
		return new FirstResults<>(size);
	}
	
}
