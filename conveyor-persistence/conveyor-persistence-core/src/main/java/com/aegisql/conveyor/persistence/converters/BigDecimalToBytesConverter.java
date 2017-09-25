package com.aegisql.conveyor.persistence.converters;

import java.math.BigDecimal;
import java.math.BigInteger;

// TODO: Auto-generated Javadoc
/**
 * The Class BigDecimalToBytesConverter.
 */
public class BigDecimalToBytesConverter implements ObjectToByteArrayConverter<BigDecimal> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(BigDecimal obj) {
		if(obj==null) {
			return null;
		}
		return obj.unscaledValue().toByteArray();
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public BigDecimal fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		BigInteger bi = new BigInteger(p);
		return new BigDecimal(bi);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "BigDecimal:byte[]";
	}


}
