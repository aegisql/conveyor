package com.aegisql.conveyor.persistence.converters.arrays;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.aegisql.conveyor.persistence.converters.BigIntegerToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class BigIntegersToBytesConverter.
 */
public class BigIntegersToBytesConverter implements ObjectArrayToByteArrayConverter<BigInteger> {
	
	/** The cc. */
	CollectionToByteArrayConverter<BigInteger> cc = new CollectionToByteArrayConverter<BigInteger>(ArrayList::new, new BigIntegerToBytesConverter());

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(BigInteger[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public BigInteger[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<BigInteger> c = cc.fromPersistence(p);
		return c.toArray(new BigInteger[0]);
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "BigInteger[]:byte[]";
	}

}
