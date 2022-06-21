package com.aegisql.conveyor.persistence.converters.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.aegisql.conveyor.persistence.converters.StringToBytesConverter;
import com.aegisql.conveyor.persistence.converters.collections.CollectionToByteArrayConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class StringsToBytesConverter.
 */
public class StringsToBytesConverter implements ObjectArrayToByteArrayConverter<String> {
	
	/** The cc. */
	CollectionToByteArrayConverter<String> cc = new CollectionToByteArrayConverter<>(ArrayList::new, new StringToBytesConverter());

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(String[] obj) {
		if(obj == null) {
			return null;
		}
		return cc.toPersistence(Arrays.asList(obj));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public String[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Collection<String> c = cc.fromPersistence(p);
		return c.toArray(new String[0]);
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "String[]:byte[]";
	}


}
