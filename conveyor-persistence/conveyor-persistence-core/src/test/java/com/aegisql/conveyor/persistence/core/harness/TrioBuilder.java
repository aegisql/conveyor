package com.aegisql.conveyor.persistence.core.harness;

import java.io.Serializable;
import java.util.function.Supplier;

public class TrioBuilder implements Supplier<Trio>, Serializable{

	
	private static final long serialVersionUID = 1L;
	private String text1;
	private String text2;
	private int number;

	
	@Override
	public Trio get() {
		return new Trio(text1,text2,number);
	}
	
	public static void setText1(TrioBuilder b,String txt) {
		b.text1 = txt;
	}
	public static void setText2(TrioBuilder b,String txt) {
		b.text2 = txt;
	}
	public static void setNumber(TrioBuilder b,Integer x) {
		b.number = x;
	}

}
