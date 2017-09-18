package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class NullConverter implements ObjectConverter<Object, byte[]> {

	@Override
	public byte[] toPersistence(Object obj) {
		return null;
	}

	@Override
	public Object fromPersistence(byte[] p) {
		return null;
	}
	
	@Override
	public String conversionHint() {
		return "null:null";
	}

}
