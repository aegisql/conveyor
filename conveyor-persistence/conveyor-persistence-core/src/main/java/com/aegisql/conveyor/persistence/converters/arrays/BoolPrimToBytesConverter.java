package com.aegisql.conveyor.persistence.converters.arrays;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

// TODO: Auto-generated Javadoc
/**
 * The Class BoolPrimToBytesConverter.
 */
public class BoolPrimToBytesConverter implements ObjectConverter<boolean[], byte[]> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(boolean[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[obj.length];
		
		
		for(int i = 0; i < obj.length; i++) {
			res[i] = (byte) (obj[i] ? 1:0);
		}
		
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public boolean[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		boolean[] res = new boolean[p.length];
		
		for(int i = 0; i < res.length; i++) {
			res[i] = p[i] != 0;
		}

		return res;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "boolean[]:byte[]";
	}


}
