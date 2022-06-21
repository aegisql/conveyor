package com.aegisql.conveyor.persistence.converters.arrays;

// TODO: Auto-generated Javadoc
/**
 * The Class BooleansToBytesConverter.
 */
public class BooleansToBytesConverter implements ObjectArrayToByteArrayConverter<Boolean> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Boolean[] obj) {
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
	public Boolean[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Boolean[] res = new Boolean[p.length];
		
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
		return "Boolean[]:byte[]";
	}


}
