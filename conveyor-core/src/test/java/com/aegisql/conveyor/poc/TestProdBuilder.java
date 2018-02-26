package com.aegisql.conveyor.poc;

import java.util.function.Supplier;

public class TestProdBuilder implements Supplier<TestProd>{

	private String a = "";
	private String b = "";

	@Override
	public TestProd get() {
		return new TestProd(a, b);
	}

	public static void setA(TestProdBuilder tpb, String a) {
		tpb.a = tpb.a+a;
	}

	public static void setB(TestProdBuilder tpb, String b) {
		tpb.b = tpb.b+b;
	}

}
