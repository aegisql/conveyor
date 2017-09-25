package com.aegisql.conveyor.persistence.converters.sql;

import java.nio.ByteBuffer;
import java.sql.Time;

import com.aegisql.conveyor.persistence.converters.ObjectToByteArrayConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class SqlTimeToBytesConverter.
 */
public class SqlTimeToBytesConverter implements ObjectToByteArrayConverter<Time> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Time obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.getTime());
		return bytes;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Time fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Time(ByteBuffer.wrap(p).getLong());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "java.sql.Time:byte[]";
	}

}
