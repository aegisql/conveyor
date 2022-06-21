package com.aegisql.conveyor.persistence.converters;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.aegisql.conveyor.persistence.core.PersistenceException;

// TODO: Auto-generated Javadoc
/**
 * The Class StringToBytesConverter.
 */
public class StringToBytesConverter implements ObjectToByteArrayConverter<String> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(String obj) {
		if(obj==null) {
			return null;
		}
		return obj.getBytes(StandardCharsets.UTF_8);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public String fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new String(p);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "String:byte[]";
	}

}
