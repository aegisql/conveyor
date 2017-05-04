package com.aegisql.conveyor.consumers.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class LastResults <K,V> implements Consumer<ProductBin<K,V>> {
	
	private final ArrayList<V> results;
	private final int last;
	private int p = 0;
	
	
	public LastResults(int size) {
		results = new ArrayList<>(size);
		this.last = size-1;
		for(int i =0;i<size;i++) {
			results.add(null);
		}
	}
	
	@Override
	public void accept(ProductBin<K, V> bin) {
		synchronized (results) {
			results.set(p, bin.product);
			if(p<last) {
				p++;
			} else {
				p=0;
			}
		}
	}
	
	public List<V> getLast() {
		ArrayList<V> lastRes = new ArrayList<>(last+1);
		synchronized (results) {
			int lp = p;
			for(int i = 0; i <= last;i++) {
				lastRes.add(results.get(lp));
				if(lp<last) {
					lp++;
				} else {
					lp=0;
				}
			}
		}
		return Collections.unmodifiableList(lastRes);
	}

	@Override
	public String toString() {
		return "LastResults [" + getLast() + "]";
	}

	public static <K,V> LastResults<K, V> of(Conveyor<K, ?, V> conv,int size) {
		return new LastResults<>(size);
	}
	
}
