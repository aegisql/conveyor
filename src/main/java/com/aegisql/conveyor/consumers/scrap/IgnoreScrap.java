package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

public class IgnoreScrap <K> implements Consumer<ScrapBin<?,?>>{

	@Override
	public void accept(ScrapBin<?, ?> t) {
		//Ignore
	}

	public static <K> IgnoreScrap<K> of(Conveyor<K, ?, ?> conveyor) {
		return new IgnoreScrap<>();
	}
	
}
