package com.aegisql.conveyor.persistence.converters;

import java.math.BigInteger;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class BigIntegerToBytesConverter implements ObjectConverter<BigInteger, byte[]> {

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

}
