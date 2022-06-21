package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * The Class FloatToBytesConverter.
 */
public class FloatToBytesConverter implements ObjectToByteArrayConverter<Float> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Float obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).putFloat(obj);
		return bytes;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Float fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getFloat();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Float:byte[]";
	}

}
