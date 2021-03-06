package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;
import java.util.UUID;

// TODO: Auto-generated Javadoc
/**
 * The Class UuidsToBytesConverter.
 */
public class UuidsToBytesConverter implements ObjectArrayToByteArrayConverter<UUID> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(UUID[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[2*8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putLong(16*i, obj[i].getMostSignificantBits());
			bb.putLong(16*i+8, obj[i].getLeastSignificantBits());
		}
		
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public UUID[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		UUID[] res = new UUID[p.length/16];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			long most  = bb.getLong(16*i);
			long least = bb.getLong(16*i+8);
			res[i] =new UUID(most, least);
		}

		return res;
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "UUID[]:byte[]";
	}


}
