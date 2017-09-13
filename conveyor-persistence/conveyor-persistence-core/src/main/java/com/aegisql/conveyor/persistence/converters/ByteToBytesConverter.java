package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class ByteToBytesConverter implements ObjectConverter<Byte, byte[]> {

	@Override
	public byte[] toPersistence(Byte obj) {
		if(obj==null) {
			return null;
		}
		return new byte[]{obj};
	}

	@Override
	public Byte fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return p[0];
	}

}
