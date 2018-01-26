package com.aegisql.conveyor.config.harness;

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
	
	public static void first(StringSupplier ss, String first) {
		System.out.println("StringSupplier first "+first);
		ss.s = first+ss.s;
	}

	public static void last(StringSupplier ss, String last) {
		System.out.println("StringSupplier last "+last);
		ss.s = ss.s+last;
	}

	@Override
	public String toString() {
		return "StringSupplier [" + (s != null ? "s=" + s : "") + "]";
	}
	

}
