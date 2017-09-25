package com.aegisql.conveyor.persistence.converters;

// TODO: Auto-generated Javadoc
/**
 * The Class BooleanToBytesConverter.
 */
public class BooleanToBytesConverter implements ObjectToByteArrayConverter<Boolean> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Boolean obj) {
		if(obj==null) {
			return null;
		}
		return new byte[]{(byte) (obj?1:0)};
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Boolean fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return p[0] != 0;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Boolean:byte[]";
	}


}
