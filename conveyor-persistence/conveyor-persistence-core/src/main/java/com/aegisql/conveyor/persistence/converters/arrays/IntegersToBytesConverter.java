package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

public class IntegersToBytesConverter implements ObjectArrayToByteArrayConverter<Integer> {

	@Override
	public byte[] toPersistence(Integer[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[4*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putInt(4*i, obj[i].intValue());
		}
		
		return res;
	}

	@Override
	public Integer[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Integer[] res = new Integer[p.length/4];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getInt(4*i);
		}

		return res;
	}

}
