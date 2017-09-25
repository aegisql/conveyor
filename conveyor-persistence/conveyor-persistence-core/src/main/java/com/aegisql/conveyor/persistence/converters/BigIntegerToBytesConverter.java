package com.aegisql.conveyor.persistence.converters;

import java.math.BigInteger;

// TODO: Auto-generated Javadoc
/**
 * The Class BigIntegerToBytesConverter.
 */
public class BigIntegerToBytesConverter implements ObjectToByteArrayConverter<BigInteger> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(BigInteger obj) {
		if(obj==null) {
			return null;
		}
		return obj.toByteArray();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public BigInteger fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new BigInteger(p);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "BigInteger:byte[]";
	}


}
