package com.aegisql.conveyor.config;

import java.util.function.Supplier;

public class StringSupplier implements Supplier<String> {

	public StringSupplier() {
		
	}
	
	@Override
	public String get() {
		return "test";
	}

}
