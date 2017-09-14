package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class LongsToBytesConverter implements ObjectConverter<Long[], byte[]> {

	@Override
	public byte[] toPersistence(Long[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[8*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putLong(8*i, obj[i].longValue());
		}
		
		return res;
	}

	@Override
	public Long[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Long[] res = new Long[p.length/8];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getLong(8*i);
		}

		return res;
	}

}
