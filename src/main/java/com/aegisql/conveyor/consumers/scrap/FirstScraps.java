package com.aegisql.conveyor.consumers.scrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class FirstScraps implements Consumer<ScrapBin<?,?>> {
	
	private final ArrayList<Object> scraps;
	private final int max;
	private int p = 0;	
	
	public FirstScraps(int size) {
		scraps = new ArrayList<>(size);
		this.max = size;
	}
	
	@Override
	public void accept(ScrapBin<?,?> bin) {
		synchronized (scraps) {
			if(p < max) {
				scraps.add(bin.scrap);
				p++;
			}
		}
	}
	
	public List<?> getFirst() {
		return Collections.unmodifiableList(scraps);
	}

	@Override
	public String toString() {
		return "FirstScraps [" + scraps + "]";
	}

	public static <K> FirstScraps of(Conveyor<K, ?, ?> conv,int size) {
		return new FirstScraps(size);
	}
	
}
