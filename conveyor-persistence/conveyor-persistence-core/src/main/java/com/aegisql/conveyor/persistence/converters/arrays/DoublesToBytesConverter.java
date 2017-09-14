package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class DoublesToBytesConverter implements ObjectConverter<Double[], byte[]> {

	@Override
	public byte[] toPersistence(Double[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putDouble(8*i, obj[i].doubleValue());
		}
		
		return res;
	}

	@Override
	public Double[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Double[] res = new Double[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getDouble(8*i);
		}

		return res;
	}

}
