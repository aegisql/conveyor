package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * The Class CharToBytesConverter.
 */
public class CharToBytesConverter implements ObjectToByteArrayConverter<Character> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Character obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[2];
		ByteBuffer.wrap(bytes).putChar(obj);
		return bytes;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Character fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getChar();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Character:byte[]";
	}

}
