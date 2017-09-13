package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;
import java.util.Date;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class DateToBytesConverter implements ObjectConverter<Date, byte[]> {

	@Override
	public byte[] toPersistence(Date obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.getTime());
		return bytes;
	}

	@Override
	public Date fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Date(ByteBuffer.wrap(p).getLong());
	}

}
