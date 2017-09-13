package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class BooleanToBytesConverter implements ObjectConverter<Boolean, byte[]> {

	@Override
	public byte[] toPersistence(Boolean obj) {
		if(obj==null) {
			return null;
		}
		return new byte[]{(byte) (obj?1:0)};
	}

	@Override
	public Boolean fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return p[0] != 0;
	}

}
