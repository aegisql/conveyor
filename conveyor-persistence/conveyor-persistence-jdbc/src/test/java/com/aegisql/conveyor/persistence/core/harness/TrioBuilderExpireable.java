package com.aegisql.conveyor.persistence.core.harness;

import java.util.function.Supplier;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioBuilderExpireable;

public class TrioBuilderExpireable implements Supplier<Trio>, Expireable{

	
	private String text1;
	private String text2;
	private int number;
	private final long expirationTime = System.currentTimeMillis() + 1000;

	
	@Override
	public Trio get() {
		return new Trio(text1,text2,number);
	}
	
	public static void setText1(TrioBuilderExpireable b,String txt) {
		b.text1 = txt;
	}
	public static void setText2(TrioBuilderExpireable b,String txt) {
		b.text2 = txt;
	}
	public static void setNumber(TrioBuilderExpireable b,Integer x) {
		b.number = x;
	}

	@Override
	public long getExpirationTime() {
		return expirationTime ;
	}

}
