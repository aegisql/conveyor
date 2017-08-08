package com.aegisql.conveyor.persistence.core;

import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

public class ThreeStageResultConsumer <K,V> implements ResultConsumer<K, V> {
	
	private ResultConsumer<K, V> before = bin->{};
	private ResultConsumer<K, V> main = bin->{};
	private ResultConsumer<K, V> after = bin->{};

	public ThreeStageResultConsumer() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void accept(ProductBin<K, V> bin) {
		before.accept(bin);
		main.accept(bin);
		after.accept(bin);
	}

}
