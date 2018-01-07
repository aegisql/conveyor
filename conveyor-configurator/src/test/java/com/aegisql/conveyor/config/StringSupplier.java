package com.aegisql.conveyor.config;

import java.util.function.Supplier;

public class StringSupplier implements Supplier<String> {

	private String s;
	
	public StringSupplier() {
		System.out.println("StringSupplier init");
		s = "TEST";
	}
	
	public StringSupplier(String init) {
		System.out.println("StringSupplier init "+init);
		s = init;
	}

	
	@Override
	public String get() {
		return s;
	}

}
