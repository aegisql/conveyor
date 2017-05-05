package com.aegisql.conveyor.consumers.scrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class LastScraps implements Consumer<ScrapBin<?,?>> {
	
	private final ArrayList<Object> results;
	private final int last;
	private int p = 0;
	private int n = -1;
	
	
	public LastScraps(int size) {
		results = new ArrayList<>(size);
		this.last = size-1;
		for(int i =0;i<size;i++) {
			results.add(null);
		}
	}
	
	@Override
	public void accept(ScrapBin<?,?> bin) {
		synchronized (results) {
			results.set(p, bin.scrap);
			if(p<last) {
				p++;
			} else {
				p=0;
			}
			if(n<last) {
				n++;
			}
		}
	}
	
	public List<?> getLast() {
		ArrayList<Object> lastRes = new ArrayList<>(last+1);
		synchronized (results) {
			int lp  = p;
			int min = Math.min(last,n);
			for(int i = 0; i <= min; i++) {
				if(lp > 0) {
					lp--;
				} else {
					lp=last;
				}
				lastRes.add(results.get(min-lp));
			}
		}
		return Collections.unmodifiableList(lastRes);
	}

	@Override
	public String toString() {
		return "LastScraps [" + getLast() + "]";
	}

	public static <K> LastScraps of(Conveyor<K, ?, ?> conv,int size) {
		return new LastScraps(size);
	}
	
}
