package com.aegisql.conveyor.persistence.core.harness;

import com.aegisql.conveyor.Expireable;

import java.util.function.Supplier;

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
