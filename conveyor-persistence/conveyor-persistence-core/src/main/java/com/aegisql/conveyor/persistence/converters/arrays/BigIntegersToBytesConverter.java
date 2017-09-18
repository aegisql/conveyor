package com.aegisql.conveyor.persistence.converters.arrays;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.aegisql.conveyor.persistence.converters.BigIntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

public class BigIntegersToBytesConverter implements ObjectArrayToByteArrayConverter<BigInteger> {
	
	CollectionToByteArrayConverter<BigInteger> cc = new CollectionToByteArrayConverter<BigInteger>(ArrayList::new, new BigIntegerToBytesConverter()){
		@Override
		public String conversionHint() {
			return "ArrayList<BigInteger>[]:byte[]";
		}
	};

	@Override
	public byte[] toPersistence(BigInteger[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	@Override
	public BigInteger[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<BigInteger> c = cc.fromPersistence(p);
		return c.toArray(new BigInteger[0]);
	}

	@Override
	public String conversionHint() {
		return "BigInteger[]:byte[]";
	}

}
