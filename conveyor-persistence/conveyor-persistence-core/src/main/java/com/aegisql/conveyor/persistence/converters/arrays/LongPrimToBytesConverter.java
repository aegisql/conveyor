package com.aegisql.conveyor.persistence.converters.arrays;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * The Class LongPrimToBytesConverter.
 */
public class LongPrimToBytesConverter implements ObjectConverter<long[], byte[]> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(long[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putLong(8*i, obj[i]);
		}
		
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public long[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		long[] res = new long[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getInt(8*i);
		}

		return res;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "long[]:byte[]";
	}


}
