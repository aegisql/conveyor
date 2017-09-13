package com.aegisql.conveyor.persistence.converters.sql;

import java.nio.ByteBuffer;
import java.sql.Time;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class SqlTimestampToBytesConverter implements ObjectConverter<Time, byte[]> {

	@Override
	public byte[] toPersistence(Time obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.getTime());
		return bytes;
	}

	@Override
	public Time fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Time(ByteBuffer.wrap(p).getLong());
	}

}
