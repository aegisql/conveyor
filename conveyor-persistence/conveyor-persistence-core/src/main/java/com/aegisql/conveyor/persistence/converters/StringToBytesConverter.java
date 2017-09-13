package com.aegisql.conveyor.persistence.converters;

import java.io.UnsupportedEncodingException;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class StringToBytesConverter implements ObjectConverter<String, byte[]> {

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

}
