package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class ShortPrimToBytesConverter implements ObjectConverter<short[], byte[]> {

	@Override
	public byte[] toPersistence(short[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[2*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putShort(2*i, obj[i]);
		}
		
		return res;
	}

	@Override
	public short[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		short[] res = new short[p.length/2];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getShort(2*i);
		}

		return res;
	}
	
	@Override
	public String conversionHint() {
		return "short[]:byte[]";
	}


}
