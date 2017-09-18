package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidToBytesConverter implements ObjectToByteArrayConverter<UUID> {

	@Override
	public byte[] toPersistence(UUID obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[2*8];
		ByteBuffer.wrap(bytes).putLong(0,obj.getMostSignificantBits());
		ByteBuffer.wrap(bytes).putLong(8,obj.getLeastSignificantBits());
		return bytes;
	}

	@Override
	public UUID fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		ByteBuffer bb = ByteBuffer.wrap(p);
		long most  = bb.getLong(0);
		long least = bb.getLong(8);
		return new UUID(most, least);
	}

	@Override
	public String conversionHint() {
		return "UUID:byte[]";
	}

}
