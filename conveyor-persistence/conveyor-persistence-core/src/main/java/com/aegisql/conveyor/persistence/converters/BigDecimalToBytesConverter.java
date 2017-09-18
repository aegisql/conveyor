package com.aegisql.conveyor.persistence.converters;

import java.math.BigDecimal;
import java.math.BigInteger;

public class BigDecimalToBytesConverter implements ObjectToByteArrayConverter<BigDecimal> {

	@Override
	public byte[] toPersistence(BigDecimal obj) {
		if(obj==null) {
			return null;
		}
		return obj.unscaledValue().toByteArray();
	}

	@Override
	public BigDecimal fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		BigInteger bi = new BigInteger(p);
		return new BigDecimal(bi);
	}
	
	@Override
	public String conversionHint() {
		return "BigDecimal:byte[]";
	}


}
