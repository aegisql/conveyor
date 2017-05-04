package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class IgnoreScrap <K> implements Consumer<ScrapBin<K,?>>{

	@Override
	public void accept(ScrapBin<K, ?> t) {
		//Ignore
	}

	public static <K> IgnoreScrap<K> of(Conveyor<K, ?, ?> conveyor) {
		return new IgnoreScrap<>();
	}
	
}
