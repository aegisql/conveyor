package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class DoublePrimToBytesConverter implements ObjectConverter<double[], byte[]> {

	@Override
	public byte[] toPersistence(double[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putDouble(8*i, obj[i]);
		}
		
		return res;
	}

	@Override
	public double[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		double[] res = new double[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getDouble(8*i);
		}

		return res;
	}

	@Override
	public String conversionHint() {
		return "double[]:byte[]";
	}

}
