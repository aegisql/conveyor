package com.aegisql.conveyor.persistence.converters;

import java.io.UnsupportedEncodingException;

public class StringToBytesConverter implements ObjectToByteArrayConverter<String> {

	@Override
	public byte[] toPersistence(String obj) {
		if(obj==null) {
			return null;
		}
		try {
			return obj.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unexpected string to bytes conversion error",e);
		}
	}

	@Override
	public String fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new String(p);
	}

	@Override
	public String conversionHint() {
		return "String:byte[]";
	}

}
