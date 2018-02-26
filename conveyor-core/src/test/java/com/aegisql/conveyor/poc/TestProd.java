package com.aegisql.conveyor.poc;

public class TestProd {

	private final String a;
	private final String b;
	
	public TestProd(String a, String b) {
		this.a = a;
		this.b = b;
	}

	public String getA() {
		return a;
	}

	public String getB() {
		return b;
	}

	@Override
	public String toString() {
		return "TestProd [" + (a != null ? "a=" + a + ", " : "") + (b != null ? "b=" + b : "") + "]";
	}
	
	
	
}
