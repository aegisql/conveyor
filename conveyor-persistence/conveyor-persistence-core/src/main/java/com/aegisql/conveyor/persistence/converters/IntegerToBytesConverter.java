package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class IntegerToBytesConverter implements ObjectConverter<Integer, byte[]> {

	@Override
	public byte[] toPersistence(Integer obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putInt(obj.intValue());
		return bytes;
	}

	@Override
	public Integer fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getInt();
	}

}
