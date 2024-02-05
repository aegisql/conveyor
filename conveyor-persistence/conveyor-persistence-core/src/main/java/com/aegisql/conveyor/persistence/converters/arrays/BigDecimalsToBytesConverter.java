package com.aegisql.conveyor.persistence.converters.arrays;

import com.aegisql.conveyor.persistence.converters.BigDecimalToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

// TODO: Auto-generated Javadoc
/**
 * The Class BigDecimalsToBytesConverter.
 */
public class BigDecimalsToBytesConverter implements ObjectArrayToByteArrayConverter<BigDecimal> {
	
	/** The cc. */
	CollectionToByteArrayConverter<BigDecimal> cc = new CollectionToByteArrayConverter<>(ArrayList::new, new BigDecimalToBytesConverter());

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(BigDecimal[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public BigDecimal[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<BigDecimal> c = cc.fromPersistence(p);
		return c.toArray(new BigDecimal[0]);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "BigDecimal[]:byte[]";
	}


}
