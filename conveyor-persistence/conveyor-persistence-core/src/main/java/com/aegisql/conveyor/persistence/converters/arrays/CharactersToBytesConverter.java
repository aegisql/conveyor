package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

// TODO: Auto-generated Javadoc
/**
 * The Class CharactersToBytesConverter.
 */
public class CharactersToBytesConverter implements ObjectArrayToByteArrayConverter<Character> {

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#toPersistence(java.lang.Object)
	 */
	@Override
	public byte[] toPersistence(Character[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[2*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putChar(2*i, obj[i]);
		}
		
		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#fromPersistence(java.lang.Object)
	 */
	@Override
	public Character[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Character[] res = new Character[p.length/2];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getChar(2*i);
		}

		return res;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.persistence.core.ObjectConverter#conversionHint()
	 */
	@Override
	public String conversionHint() {
		return "Character[]:byte[]";
	}

}
