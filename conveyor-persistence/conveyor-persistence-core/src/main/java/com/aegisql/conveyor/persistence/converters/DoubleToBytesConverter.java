package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

public class DoubleToBytesConverter implements ObjectToByteArrayConverter<Double> {

	@Override
	public byte[] toPersistence(Double obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putDouble(obj.doubleValue());
		return bytes;
	}

	@Override
	public Double fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getDouble();
	}

	@Override
	public String conversionHint() {
		return "Double:byte[]";
	}

}
