package com.aegisql.conveyor.persistence.converters.sql;

import com.aegisql.conveyor.persistence.converters.ObjectToByteArrayConverter;

import java.nio.ByteBuffer;
import java.sql.Date;

// TODO: Auto-generated Javadoc
/**
 * The Class SqlDateToBytesConverter.
 */
public class SqlDateToBytesConverter implements ObjectToByteArrayConverter<Date> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Date obj) {
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
	public Date fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Date(ByteBuffer.wrap(p).getLong());
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "java.sql.Date:byte[]";
	}

}
