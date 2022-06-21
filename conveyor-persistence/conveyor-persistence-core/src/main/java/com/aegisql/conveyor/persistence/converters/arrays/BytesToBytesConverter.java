package com.aegisql.conveyor.persistence.converters.arrays;

// TODO: Auto-generated Javadoc
/**
 * The Class BytesToBytesConverter.
 */
public class BytesToBytesConverter implements ObjectArrayToByteArrayConverter<Byte> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Byte[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[obj.length];
		for(int i=0; i< obj.length; i++){
			res[i] = obj[i];
		}
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Byte[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Byte[] res = new Byte[p.length];
		for(int i=0; i< p.length; i++){
			res[i] = p[i];
		}
		return res;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Byte[]:byte[]";
	}


}
