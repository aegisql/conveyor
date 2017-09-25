package com.aegisql.conveyor.persistence.converters;

// TODO: Auto-generated Javadoc
/**
 * The Class ByteToBytesConverter.
 */
public class ByteToBytesConverter implements ObjectToByteArrayConverter<Byte> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Byte obj) {
		if(obj==null) {
			return null;
		}
		return new byte[]{obj};
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Byte fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return p[0];
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Byte:byte[]";
	}

}
