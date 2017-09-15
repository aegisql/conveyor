package com.aegisql.conveyor.persistence.converters.sql;

import java.nio.ByteBuffer;
import java.sql.Timestamp;

import com.aegisql.conveyor.persistence.converters.ObjectToByteArrayConverter;

public class SqlTimestampToBytesConverter implements ObjectToByteArrayConverter<Timestamp> {

	@Override
	public byte[] toPersistence(Timestamp obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.getTime());
		return bytes;
	}

	@Override
	public Timestamp fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Timestamp(ByteBuffer.wrap(p).getLong());
	}

}
