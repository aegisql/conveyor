package com.aegisql.conveyor.persistence.converters;

import java.math.BigInteger;

public class BigIntegerToBytesConverter implements ObjectToByteArrayConverter<BigInteger> {

	@Override
	public byte[] toPersistence(BigInteger obj) {
		if(obj==null) {
			return null;
		}
		return obj.toByteArray();
	}

	@Override
	public BigInteger fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new BigInteger(p);
	}
	
	@Override
	public String conversionHint() {
		return "BigInteger:byte[]";
	}


}
