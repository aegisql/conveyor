package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * The Class LongToBytesConverter.
 */
public class LongToBytesConverter implements ObjectToByteArrayConverter<Long> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Long obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.longValue());
		return bytes;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Long fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getLong();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Long:byte[]";
	}

}
