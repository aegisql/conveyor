package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

public class CharToBytesConverter implements ObjectToByteArrayConverter<Character> {

	@Override
	public byte[] toPersistence(Character obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[2];
		ByteBuffer.wrap(bytes).putChar(obj.charValue());
		return bytes;
	}

	@Override
	public Character fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getChar();
	}

}
