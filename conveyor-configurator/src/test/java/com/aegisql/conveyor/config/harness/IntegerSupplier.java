package com.aegisql.conveyor.config.harness;

import java.util.function.Supplier;

public class IntegerSupplier implements Supplier<Integer> {

	private Integer s;
	
	public IntegerSupplier() {
		System.out.println("StringSupplier init");
		s = 1;
	}
	
	public IntegerSupplier(Integer init) {
		System.out.println("IntegerSupplier init "+init);
		s = init;
	}

	
	@Override
	public Integer get() {
		return s;
	}

}
