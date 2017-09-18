package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

public class FloatsToBytesConverter implements ObjectArrayToByteArrayConverter<Float> {

	@Override
	public byte[] toPersistence(Float[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[4*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putFloat(4*i, obj[i].floatValue());
		}
		
		return res;
	}

	@Override
	public Float[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Float[] res = new Float[p.length/4];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getFloat(4*i);
		}

		return res;
	}

	@Override
	public String conversionHint() {
		return "Float[]:byte[]";
	}

}
