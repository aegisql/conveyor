package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class CharPrimToBytesConverter implements ObjectConverter<char[], byte[]> {

	@Override
	public byte[] toPersistence(char[] obj) {
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

	@Override
	public char[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		char[] res = new char[p.length/2];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getChar(2*i);
		}

		return res;
	}

}
