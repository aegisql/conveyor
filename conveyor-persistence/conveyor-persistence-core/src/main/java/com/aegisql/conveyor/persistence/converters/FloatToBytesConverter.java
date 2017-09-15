package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

public class FloatToBytesConverter implements ObjectToByteArrayConverter<Float> {

	@Override
	public byte[] toPersistence(Float obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putFloat(obj.floatValue());
		return bytes;
	}

	@Override
	public Float fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getFloat();
	}

}
