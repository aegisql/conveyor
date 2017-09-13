package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class LongToBytesConverter implements ObjectConverter<Long, byte[]> {

	@Override
	public byte[] toPersistence(Long obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.longValue());
		return bytes;
	}

	@Override
	public Long fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getLong();
	}

}
