package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class FloatPrimToBytesConverter implements ObjectConverter<float[], byte[]> {

	@Override
	public byte[] toPersistence(float[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[4*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putFloat(4*i, obj[i]);
		}
		
		return res;
	}

	@Override
	public float[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		float[] res = new float[p.length/4];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getFloat(4*i);
		}

		return res;
	}
	
	@Override
	public String conversionHint() {
		return "float[]:byte[]";
	}


}
