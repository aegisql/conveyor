package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.PersistenceException;

public class ClassToBytesConverter implements ObjectToByteArrayConverter<Class> {

	public ClassToBytesConverter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public byte[] toPersistence(Class obj) {
		return obj.getName().getBytes();
	}

	@Override
	public Class fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		try {
			return Class.forName(new String(p));
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(conversionHint()+" conversion error",e);
		}
	}

	@Override
	public String conversionHint() {
		return "Class:byte[]";
	}

}
