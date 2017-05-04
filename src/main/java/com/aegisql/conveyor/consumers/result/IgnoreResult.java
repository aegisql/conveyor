package com.aegisql.conveyor.consumers.result;

import java.util.function.Consumer;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;

public class IgnoreResult <K,E> implements Consumer<ProductBin<K,E>> {

	@Override
	public void accept(ProductBin<K, E> t) {
		//Ignore
	}

	public static <K,E> IgnoreResult<K,E> of(Conveyor<K, ?, E> conveyor) {
		return new IgnoreResult<>();
	}

}
