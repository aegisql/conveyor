package com.aegisql.conveyor.persistence.converters.arrays;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.aegisql.conveyor.persistence.converters.BigDecimalToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

public class BigDecimalsToBytesConverter implements ObjectArrayToByteArrayConverter<BigDecimal> {
	
	CollectionToByteArrayConverter<BigDecimal> cc = new CollectionToByteArrayConverter<BigDecimal>(ArrayList::new, new BigDecimalToBytesConverter());

	@Override
	public byte[] toPersistence(BigDecimal[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	@Override
	public BigDecimal[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<BigDecimal> c = cc.fromPersistence(p);
		return c.toArray(new BigDecimal[0]);
	}
	
	@Override
	public String conversionHint() {
		return "BigDecimal[]:byte[]";
	}


}
