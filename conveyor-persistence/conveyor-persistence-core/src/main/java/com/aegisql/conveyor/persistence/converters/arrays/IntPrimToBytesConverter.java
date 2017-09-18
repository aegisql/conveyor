package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class IntPrimToBytesConverter implements ObjectConverter<int[], byte[]> {

	@Override
	public byte[] toPersistence(int[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[4*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putInt(4*i, obj[i]);
		}
		
		return res;
	}

	@Override
	public int[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		int[] res = new int[p.length/4];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getInt(4*i);
		}

		return res;
	}

	@Override
	public String conversionHint() {
		return "int[]:byte[]";
	}

}
